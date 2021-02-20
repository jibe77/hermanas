package org.jibe77.hermanas.controller.camera;

import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.data.entity.Picture;
import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class CameraController {

    private LightController lightController;

    private boolean lightSwitchedOnByCamera;

    private GpioHermanasController gpioHermanasController;

    private PictureRepository pictureRepository;

    private DoorPictureAnalizer doorPictureAnalizer;

    @Value("${camera.path.root}")
    private String rootPath;

    Logger logger = LoggerFactory.getLogger(CameraController.class);

    @Value("${camera.streaming.command}")
    private String streamingCommand;

    public CameraController(LightController lightController, GpioHermanasController gpioHermanasController,
                            PictureRepository pictureRepository, ProcessLauncher processLauncher,
                            DoorPictureAnalizer doorPictureAnalizer) {
        this.lightController = lightController;
        this.gpioHermanasController = gpioHermanasController;
        this.pictureRepository = pictureRepository;
        this.processLauncher = processLauncher;
        this.doorPictureAnalizer = doorPictureAnalizer;
    }

    public synchronized File takePicture(boolean highQuality) throws IOException, InterruptedException {
        if (currentStreamingProcess != null) {
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
            gpioHermanasController.takePicture(new FilePictureCaptureHandler(pictureFile), highQuality);
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
                (suffix == 1 ? "" : "-"+suffix) + ".png";
    }

    /**
     * Switch off the light if the light was not already switched on by the current controller.
     */
    private void switchOffLight() {
        if (lightSwitchedOnByCamera) {
            lightController.switchOff();
            lightSwitchedOnByCamera = false;
        } else {
            logger.debug("light hasn't been switched on before taking picture, it should not be switched off.");
        }
    }

    /**
     * Switch on the light managing the previous state of the light.
     */
    private void switchLightOn() {
        if (StatusEnum.ON.equals(lightController.getStatus().getStatusEnum())) {
            logger.debug("light is already on, it's useless to switch it on again.");
            lightSwitchedOnByCamera = false;
        } else {
            logger.info("light has been switched on by camera.");
            lightController.switchOn();
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

    ProcessLauncher processLauncher;

    private Process currentStreamingProcess;

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
        logger.info("client has disconnected, it remains {} clients now.");
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
