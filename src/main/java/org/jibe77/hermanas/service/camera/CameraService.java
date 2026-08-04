package org.jibe77.hermanas.service.camera;

import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.service.ProcessLauncher;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.data.entity.Picture;
import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class CameraService {

    private LightService lightService;

    private boolean lightSwitchedOnByCamera;

    private GpioHermanasService gpioHermanasService;

    private PictureRepository pictureRepository;

    private DoorPictureAnalizer doorPictureAnalizer;

    private ProcessLauncher processLauncher;

    private Process currentStreamingProcess;

    @Value("${camera.path.root}")
    private String rootPath;

    /**
     * Short-lived cache to share the same JPEG between near-simultaneous
     * callers (e.g. the {@code <img>} on the Webcam page and the AI analysis
     * triggered right after it loads). Two entries max — one per
     * {@code highQuality} flavour.
     */
    @Value("${camera.cache.ttl-ms:30000}")
    private long pictureCacheTtlMs;

    private final java.util.Map<Boolean, CachedPicture> pictureCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);

    @Value("${camera.streaming.command}")
    private String streamingCommand;

    public CameraService(LightService lightService, GpioHermanasService gpioHermanasService,
                            PictureRepository pictureRepository, ProcessLauncher processLauncher,
                            DoorPictureAnalizer doorPictureAnalizer) {
        this.lightService = lightService;
        this.gpioHermanasService = gpioHermanasService;
        this.pictureRepository = pictureRepository;
        this.processLauncher = processLauncher;
        this.doorPictureAnalizer = doorPictureAnalizer;
    }

    public synchronized File takePicture(boolean highQuality) throws IOException, InterruptedException {
        if (currentStreamingProcess != null) {
            logger.info("stopping current stream before taking picture.");
            stopStream();
        }
        logger.info("taking a picture in root path {}.", rootPath);
        switchLightOn();
        LocalDateTime localDateTime = LocalDateTime.now();
        String relativePath = generateRelativePath(localDateTime);
        File fileRoot = new File(rootPath + File.separator + relativePath);
        FileUtils.forceMkdir(fileRoot);
        File pictureFile = generateUniqueFilename(localDateTime, fileRoot);
        logger.info("Taking a picture now in {} ...", pictureFile.getAbsolutePath());
        try {
            gpioHermanasService.takePicture(pictureFile, highQuality);
            logger.info("Save picture path in db.");
            pictureRepository.save(new Picture(relativePath + File.separator + pictureFile.getName()));
            logger.info("... done.");
            return pictureFile;
        } catch (IOException e) {
            throw new IOException("Can't take picture or fetch file.", e);
        } finally {
            switchOffLight();
        }
    }

    private File generateUniqueFilename(LocalDateTime localDateTime, File fileRoot) {
        return generateUniqueFilename(localDateTime, 1, fileRoot);
    }

    private File generateUniqueFilename(LocalDateTime localDateTime, int suffix, File fileRoot) {
        String filename = generateFilename(localDateTime, suffix);
        File file = new File(fileRoot, filename);
        if (file.exists()) {
            return generateUniqueFilename(localDateTime, suffix+1, fileRoot);
        }
        return file;
    }

    private String generateRelativePath(LocalDateTime localDateTime) {
        return localDateTime.getYear() + "/" +
                localDateTime.getMonthValue() + "/" +
                localDateTime.getDayOfMonth();
    }

    private String generateFilename(LocalDateTime localDateTime, int suffix) {
        return localDateTime.getYear() + "-" + localDateTime.getMonthValue() + "-" +
                localDateTime.getDayOfMonth() + "-" + localDateTime.getHour() + "-" +
                localDateTime.getMinute() +
                (suffix == 1 ? "" : "-"+suffix) + ".jpg";
    }

    /**
     * Switch off the light if the light was not already switched on by the current controller.
     */
    private void switchOffLight() {
        if (lightSwitchedOnByCamera) {
            lightService.switchOff();
            lightSwitchedOnByCamera = false;
        } else {
            logger.debug("light hasn't been switched on before taking picture, it should not be switched off.");
        }
    }

    /**
     * Switch on the light managing the previous state of the light.
     */
    private void switchLightOn() {
        if (StatusEnum.ON.equals(lightService.getStatus().getStatusEnum())) {
            logger.debug("light is already on, it's useless to switch it on again.");
            lightSwitchedOnByCamera = false;
        } else {
            logger.info("light has been switched on by camera.");
            lightService.switchOn();
            lightSwitchedOnByCamera = true;
        }
    }

    /**
     * Takes a picture managing the IO exception.
     */
    public Optional<File> takePictureNoException(boolean highQuality) {
        try {
            return Optional.ofNullable(takePicture(highQuality));
        } catch (IOException | InterruptedException e) {
            logger.error("Can't take picture.", e);
            return Optional.empty();
        }
    }

    /**
     * Variant of {@link #takePicture(boolean)} that returns the previous
     * capture (per {@code highQuality} flavour) when it is younger than
     * {@code camera.cache.ttl-ms} and the file still exists on disk. Lets the
     * Webcam page <img> and the AI analyze button share a single physical
     * capture rather than fire the camera twice in a row.
     *
     * <p>The {@code force} flag bypasses the cache, used by the "refresh"
     * button on the Webcam page so the operator can demand a fresh shot.</p>
     *
     * <p>Scheduler-triggered captures and event-driven captures keep using
     * {@link #takePicture(boolean)} directly so they always produce a fresh
     * archive picture.</p>
     */
    public File takePictureCached(boolean highQuality, boolean force)
            throws IOException, InterruptedException {
        if (!force) {
            File cached = freshFromCache(highQuality);
            if (cached != null) {
                return cached;
            }
        }

        // Verrou partagé avec takePicture() : les appelants concurrents attendent ici
        // plutôt que d'enchaîner les captures.
        synchronized (this) {
            // Double contrôle — l'essentiel de ce bloc.
            //
            // Le dashboard interroge cet endpoint toutes les deux secondes, alors
            // qu'une capture prend 5 à 10 s. Quand le cache expire, plusieurs requêtes
            // franchissent le test ci-dessus puis s'empilent sur le verrou. Sans cette
            // seconde vérification, chacune déclenchait sa propre capture en sortant
            // d'attente : lumière rallumée, caméra resollicitée, photos parasites en
            // rafale — alors que la première avait déjà produit l'image voulue.
            //
            // On relit donc le cache une fois le verrou obtenu : les suivantes
            // repartent avec la photo fraîchement prise.
            if (!force) {
                File cached = freshFromCache(highQuality);
                if (cached != null) {
                    logger.info("Capture évitée : une autre requête venait de produire {}.",
                            cached.getName());
                    return cached;
                }
            }

            File fresh = takePicture(highQuality);
            if (pictureCacheTtlMs > 0) {
                pictureCache.put(highQuality, new CachedPicture(fresh, System.currentTimeMillis()));
            }
            return fresh;
        }
    }

    /**
     * Entrée de cache encore valable pour ce profil, ou {@code null}.
     *
     * <p>Vérifie que le fichier existe toujours : le cache ne survit pas à une
     * suppression manuelle sous l'application.</p>
     */
    private File freshFromCache(boolean highQuality) {
        CachedPicture cached = pictureCache.get(highQuality);
        if (cached != null && cached.isFresh(pictureCacheTtlMs) && cached.file.exists()) {
            logger.info("Picture cache hit (highQuality={}, age={} ms, file={}).",
                    highQuality, System.currentTimeMillis() - cached.timestamp,
                    cached.file.getAbsolutePath());
            return cached.file;
        }
        return null;
    }

    /**
     * Drops every cached entry. Exposed for the admin-side "refresh
     * configuration" action and reused by tests.
     */
    public void clearPictureCache() {
        pictureCache.clear();
    }

    private static final class CachedPicture {
        final File file;
        final long timestamp;

        CachedPicture(File file, long timestamp) {
            this.file = file;
            this.timestamp = timestamp;
        }

        boolean isFresh(long ttlMs) {
            return ttlMs > 0 && System.currentTimeMillis() - timestamp <= ttlMs;
        }
    }

    public void stream() throws IOException {
        if (currentStreamingProcess == null) {
            logger.info("current streaming process is null, starting processs.");
            switchLightOn();
            currentStreamingProcess = processLauncher.launch(
                    "/bin/bash", "-c",
                    streamingCommand
            );
            processLauncher.printErrorStreamInThread(currentStreamingProcess);
        } else {
            logger.info("current steaming process is not null, nothing to start.");
        }
    }

    public void stopStream() throws InterruptedException, IOException {
        switchOffLight();
        logger.info("client has disconnected, it remains no client now.");
        if (currentStreamingProcess != null) {
            try {
                logger.info("Stop stream destroying process.");
                currentStreamingProcess.destroy();
                boolean hasExited = currentStreamingProcess.waitFor(3, TimeUnit.SECONDS);
                logger.info("Process has exited {}.", hasExited);
                if (!hasExited) {
                    logger.info("Force destroy.");
                    currentStreamingProcess.destroyForcibly();
                }
                logger.info("Process has exited {}, is alive {}, exit value {}",
                        hasExited, currentStreamingProcess.isAlive(), currentStreamingProcess.exitValue());
            } finally {
                processLauncher.launch("/bin/kill", "-9", Long.toString(currentStreamingProcess.pid()));
                currentStreamingProcess = null;
            }
        }
    }

    public int getClosingRate() {
        logger.info("Returning the door closing rate.");
        Optional<File> picture = takePictureNoException(true);
        if (picture.isPresent()) {
            try {
                int result = doorPictureAnalizer.getClosedStatus(picture.get());
                logger.info("return {}.", result);
                return result;
            } catch (IOException e) {
                logger.error("Can't read picture.", e);
            }
        }
        return -1;
    }
}
