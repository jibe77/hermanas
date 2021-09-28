package org.jibe77.hermanas.controller.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import uk.co.caprica.picam.*;
import uk.co.caprica.picam.CameraConfiguration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiController implements GpioHermanasController {

    private Context pi4j;

    private CameraConfiguration highQualityConfig;

    private CameraConfiguration regularQualityconfig;

    private Pwm pwm;

    Logger logger = LoggerFactory.getLogger(GpioHermanasRpiController.class);

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    @Value("${door.servo.gpio.range}")
    private int doorSettingRange;

    @Value("${camera.regular.delay}")
    private int photoRegularDelay;

    @Value("${camera.high.delay}")
    private int photoHighDelay;

    @Value("${camera.picam.jni.implementation}")
    private String picamJniImplementation;

    public GpioHermanasRpiController(@Qualifier("CameraHighQualityConfig") CameraConfiguration highQualityConfig,
                                     @Qualifier("CameraRegularQualityConfig") CameraConfiguration regularQualityconfig) {
        this.highQualityConfig = highQualityConfig;
        this.regularQualityconfig = regularQualityconfig;
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

            //Set the PinNumber pin to be a PWM pin, with values changing from 0 to 250
            //this will give enough resolution to the servo motor
            PwmConfig pwmConfig = Pwm.newConfigBuilder(pi4j)
                    .id("servo")
                    .name("Servo")
                    .address(doorServoGpioAddress)
                    .pwmType(PwmType.SOFTWARE)
                    .initial(0)
                    .shutdown(0)
                    .provider("pigpio-pwm")
                    .build();
            this.pwm = pi4j.create(pwmConfig);

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
        try (Camera camera = new Camera(highQuality ? highQualityConfig : regularQualityconfig)) {
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

    public void moveServo(int doorServoGpioAddress, int positionNumber) {
        //send the value to the motor.
        pwm.on(doorSettingRange, positionNumber);
    }

    public DigitalInput provisionInput(String id, String name, int gpioAddress) {
        DigitalInputConfigBuilder d = DigitalInput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .address(gpioAddress)
                .pull(PullResistance.PULL_DOWN)
                .debounce(3000L)
                .provider("pigpio-digital-input");
        return pi4j.create(d);

        //return gpio.provisionDigitalInputPin(
        //        RaspiPin.getPinByAddress(gpioAddress), PinPullResistance.PULL_DOWN);
    }

    @Override
    public DigitalOutput provisionOutput(String id, String name, int gpioAddress) {
        DigitalOutputConfigBuilder d = DigitalOutput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .address(gpioAddress)
                .initial(DigitalState.LOW)
                .shutdown(DigitalState.LOW)
                .provider("pigpio-digital-output"); // or raspberrypi-digital-output
        DigitalOutput digitalOutput = pi4j.create(d);
        digitalOutput.addListener(event -> {
            logger.info("Event on {} on address {}, state is now {}",
                    event.source().getId(), event.source().getAddress(), event.state());
        });
        return digitalOutput;
    }
}
