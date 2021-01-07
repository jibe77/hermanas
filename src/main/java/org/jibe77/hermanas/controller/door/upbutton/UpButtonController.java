package org.jibe77.hermanas.controller.door.upbutton;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UpButtonController {

    final ServoMotorController servoMotorController;

    final
    GpioHermanasController gpioHermanasController;

    @Value("${door.button.up.gpio.address}")
    private int doorButtonUpGpioAddress;

    private GpioPinDigitalInput upButton;

    boolean upButtonHasBeenPressed = false;

    Logger logger = LoggerFactory.getLogger(UpButtonController.class);

    public UpButtonController(ServoMotorController servoMotorController, GpioHermanasController gpioHermanasController) {
        this.servoMotorController = servoMotorController;
        this.gpioHermanasController = gpioHermanasController;
    }

    public synchronized void provisionButton() {
        if (upButton == null) {
            logger.info("provision door button on gpio instance.");
            upButton = gpioHermanasController.provisionInput(doorButtonUpGpioAddress);
            upButton.setShutdownOptions(true);
            upButton.addListener(new UpButtonListener(this, servoMotorController));
        }
    }

    public synchronized void unprovisionButton() {
        resetUpButtonState();
        if (upButton != null) {
            logger.info("unprovision door button on gpio instance.");
            upButton.removeAllListeners();
            gpioHermanasController.unprovisionPin(upButton);
            upButton = null;
        }
    }

    public synchronized boolean isUpButtonPressed() {
        boolean isButtonNull = upButton == null;
        if (isButtonNull) {
            provisionButton();
        }
        boolean isHigh = upButton.isHigh();
        if (isButtonNull) {
            unprovisionButton();
        }
        return isHigh;
    }

    public void resetUpButtonState() {
        upButtonHasBeenPressed = false;
    }

    public boolean isUpButtonHasBeenPressed() {
        return upButtonHasBeenPressed;
    }

    void setUpButtonHasBeenPressed(boolean bottomButtonHasBeenPressed) {
        this.upButtonHasBeenPressed = bottomButtonHasBeenPressed;
    }
}
