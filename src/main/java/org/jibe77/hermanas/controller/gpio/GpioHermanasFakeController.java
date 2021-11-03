package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.pwm.Pwm;
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
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        logger.info("Fake GPIO : take picture ");
        if (!cameraIsInitialised) {
            throw new IllegalStateException("Fake GPIO : can't hasn't been initialised before taking picture !");
        }
    }

    public DigitalInput provisionInput(String id, String name, int gpioAddress) {
        logger.info("Fake GPIO : provision input pin on GPIO address {}.", gpioAddress);
        return new DefaultGpioPinDigitalInput();
    }

    @Override
    public DigitalOutput provisionOutput(String id, String name, int gpioAddress) {
        logger.info("Fake GPIO : provision output pin on GPIO address {}.", gpioAddress);
        return new DefaultGpioPinDigitalOutput();
    }

    @Override
    public Pwm provisionPwm(String id, String name, int gpioAddress) {
        return new DefaultGpioPwm();
    }
}
