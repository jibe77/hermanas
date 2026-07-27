package org.jibe77.hermanas.service.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import uk.co.caprica.picam.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiService implements GpioHermanasService {

    private Context pi4j;

    private final CameraConfiguration cameraConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(GpioHermanasRpiService.class);

    @Value("${camera.regular.delay}")
    private int photoRegularDelay;

    @Value("${camera.high.delay}")
    private int photoHighDelay;

    @Value("${camera.picam.jni.implementation}")
    private String picamJniImplementation;

    public GpioHermanasRpiService(CameraConfiguration cameraConfiguration) {
        this.cameraConfiguration = cameraConfiguration;
    }

    @PostConstruct
    private void initialiseGpioPins() {
        logger.info("Initialise GPIO ...");
        try {
            logger.info("Load picam JNI implementation from .so file {}.", picamJniImplementation);

            // Loading native implementation doesn't work from spring boot fatjarù
            // PicamNativeLibrary.installTempLibrary();
            // Here is a workaround, consisting in charging extracted .so from filesystem.
            System.load(picamJniImplementation);
            logger.info("Init pi4j context.");
            pi4j = Pi4J.newAutoContext();
        } catch (UnsatisfiedLinkError e) {
            logger.error("Can't find wiringpi, is it installed on your machine ?", e);
        }
        logger.info("... initialisation done.");
    }

    @Override
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        CompletableFuture<Void> future = takePictureAsync(filePictureCaptureHandler, highQuality);
        try {
            future.get(10, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Async
    public CompletableFuture<Void> takePictureAsync(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        uk.co.caprica.picam.CameraConfiguration picamConfig = highQuality
                ? cameraConfiguration.buildHighQuality()
                : cameraConfiguration.buildRegularQuality();
        try (Camera camera = new Camera(picamConfig)) {
            camera.takePicture(filePictureCaptureHandler, highQuality ? photoHighDelay : photoRegularDelay);
        } catch (CaptureFailedException e) {
            throw new IOException("Can't capture a picture.", e);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @PreDestroy
    private void tearDown() {
        logger.info("Shutdown gpio instance.");
        pi4j.shutdown();
    }

    public DigitalInput provisionInput(String id, String name, int gpioAddress) {
        DigitalInputConfigBuilder d = DigitalInput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .bcm(gpioAddress)
                .pull(PullResistance.PULL_DOWN)
                .debounce(3000L)
                .provider("ffm-digital-input");
        return pi4j.create(d);
    }

    @Override
    public DigitalOutput provisionOutput(String id, String name, int gpioAddress) {
        DigitalOutputConfigBuilder d = DigitalOutput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .bcm(gpioAddress)
                .initial(DigitalState.LOW)
                .shutdown(DigitalState.LOW)
                .provider("ffm-digital-output");
        DigitalOutput digitalOutput = pi4j.create(d);
        digitalOutput.addListener(event -> {
            logger.info("Event on {} on address {}, state is now {}",
                    event.source().getId(), event.source().bcm(), event.state());
        });
        return digitalOutput;
    }

    // NOTE : le plugin FFM ne supporte QUE PwmType.HARDWARE. Le PWM logiciel de
    // pi4j 2.x/pigpio n'existe plus. Sur Pi Zero 2 W, seuls GPIO 12, 13, 18 et 19
    // exposent du PWM matériel — le servo est donc câblé sur GPIO 12 (broche
    // physique 32) au lieu de GPIO 25 (broche 22) qui n'en fait pas partie.
    // GPIO 18 est déjà pris par door.button.up. Cf. door.servo.gpio.address.
    public Pwm provisionPwm(String id, String name, int gpioAddress) {
        PwmConfig pwmConfig = Pwm.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .bcm(gpioAddress)
                .pwmType(PwmType.HARDWARE)
                .initial(0)
                .shutdown(0)
                .provider("ffm-pwm")
                .build();
        return pi4j.create(pwmConfig);
    }
}
