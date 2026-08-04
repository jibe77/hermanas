package org.jibe77.hermanas.service.camera;

import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.jibe77.hermanas.service.ProcessLauncher;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.light.LightService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Vérifie que des requêtes simultanées se partagent une seule capture.
 *
 * <p><b>Le défaut d'origine.</b> Le dashboard interroge {@code /takePicture} toutes
 * les deux secondes, alors qu'une capture prend 5 à 10 s sur le Pi Zero 2 W. À
 * l'expiration du cache, plusieurs requêtes franchissaient le test de fraîcheur
 * puis s'empilaient sur le {@code synchronized} de {@code takePicture}. Chacune
 * déclenchait ensuite <em>sa propre</em> capture en sortant d'attente : lumière
 * rallumée, caméra resollicitée, photos parasites en rafale — alors que la
 * première avait déjà produit l'image demandée.</p>
 *
 * <p>Le remède est un double contrôle : relire le cache une fois le verrou obtenu.</p>
 */
class CameraServiceConcurrencyTest {

    @TempDir
    Path tempDir;

    private CameraService newService(AtomicInteger captureCount) throws Exception {
        GpioHermanasService gpio = mock(GpioHermanasService.class);
        // Capture simulée : lente, et écrit réellement le fichier attendu.
        doAnswer(invocation -> {
            captureCount.incrementAndGet();
            File destination = invocation.getArgument(0);
            Thread.sleep(300);
            java.nio.file.Files.write(destination.toPath(), new byte[] {1, 2, 3});
            return null;
        }).when(gpio).takePicture(any(File.class), anyBoolean());

        // switchLightOn() interroge l'état courant : sans stub, le mock renvoie null.
        LightService light = mock(LightService.class);
        when(light.getStatus()).thenReturn(new Status(StatusEnum.OFF, 0));

        CameraService service = new CameraService(
                light,
                gpio,
                mock(PictureRepository.class),
                mock(ProcessLauncher.class),
                mock(DoorPictureAnalizer.class));
        ReflectionTestUtils.setField(service, "rootPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "pictureCacheTtlMs", 30_000L);
        return service;
    }

    @Test
    @DisplayName("Dix requêtes simultanées ne déclenchent qu'une seule capture")
    void concurrentCallersShareASingleCapture() throws Exception {
        AtomicInteger captureCount = new AtomicInteger();
        CameraService service = newService(captureCount);

        int callers = 10;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        try {
            // Toutes les requêtes partent ensemble, cache vide : sans le double
            // contrôle, chacune produisait sa propre photo.
            var tasks = IntStream.range(0, callers)
                    .<Callable<File>>mapToObj(i -> () -> service.takePictureCached(false, false))
                    .collect(Collectors.toList());

            var results = pool.invokeAll(tasks, 30, TimeUnit.SECONDS);
            for (Future<File> result : results) {
                assertTrue(result.get().isFile(), "chaque appelant doit recevoir un fichier lisible");
            }

            assertEquals(1, captureCount.get(),
                    "une seule capture physique attendue : les requêtes concurrentes doivent "
                            + "se partager la photo produite par la première, pas en déclencher "
                            + "une chacune");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("force=true déclenche bien une capture, cache ou pas")
    void forceAlwaysCapturesAfresh() throws Exception {
        AtomicInteger captureCount = new AtomicInteger();
        CameraService service = newService(captureCount);

        service.takePictureCached(false, false);   // remplit le cache
        service.takePictureCached(false, true);    // le bouton « rafraîchir »

        assertEquals(2, captureCount.get(),
                "force=true doit contourner le cache — sinon le bouton de rafraîchissement "
                        + "de la page Webcam ne servirait à rien");
    }
}
