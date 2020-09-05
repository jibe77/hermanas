package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Controller
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-fake")
public class GpioHermanasFakeController implements GpioHermanasController {

    Logger logger = LoggerFactory.getLogger(GpioHermanasFakeController.class);

    private boolean cameraIsInitialised;

    @PostConstruct
    private void initialise() {
        logger.warn("Initialise fake GPIO.");
    }

    @PreDestroy
    private void tearDown() {
        logger.info("Shutdown fake GPIO.");
    }

    public void moveServo(int doorServoGpioAddress, int positionNumber) {
        //send the value to the motor.
        logger.info("Fake GPIO : moving servo motor on Pin address {} on gear position {}.",
                doorServoGpioAddress, positionNumber);
    }

    @Override
    public void initCamera(int photoWidth, int photoHeight, String photoEncoding, int photoQuality, int photoDelay, int photoRotation) {
        logger.info(
            "Fake GPIO : init camera with photoWidth {} photoHeight {} photoEncoding {} photoQuality {} photoDelay {} photoRotation {}",
                photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay, photoRotation);
        cameraIsInitialised = true;
    }

    @Override
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler) throws IOException {
        logger.info("Fake GPIO : take picture ");
        if (!cameraIsInitialised) {
            throw new IllegalStateException("Fake GPIO : can't hasn't been initialised before taking picture !");
        }
    }

    public GpioPinDigitalInput provisionInput(int gpioAddress) {
        logger.info("Fake GPIO : provision input pin on GPIO address {}.", gpioAddress);
        return new DefaultGpioPinDigital();
    }

    public void unprovisionPin(GpioPin gpioPin) {
        logger.info("Fake GPIO : unprovision pin {}.", gpioPin);
    }

    @Override
    public GpioPinDigitalOutput provisionOutput(int gpioAddress) {
        logger.info("Fake GPIO : provision output pin on GPIO address {}.", gpioAddress);
        return new DefaultGpioPinDigital();
    }

    @Override
    public void initSensor(int pinNumber) {
        logger.info("init sensor on gpio address {}.", pinNumber);
    }

    @Override
    public void sendStartSignal(int pinNumber) {
        logger.info("send start signal to sensor on pin number {}.", pinNumber);
    }
}
