package org.jibe77.hermanas.controller.door.bottombutton;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BottomButtonController {

    final
    ServoMotorController servoMotorController;

    final
    GpioHermanasController gpioHermanasController;

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomGpioAddress;

    private GpioPinDigitalInput bottomButton;

    boolean bottomButtonHasBeenPressed = false;

    Logger logger = LoggerFactory.getLogger(BottomButtonController.class);

    public BottomButtonController(ServoMotorController servoMotorController, GpioHermanasController gpioHermanasController) {
        this.servoMotorController = servoMotorController;
        this.gpioHermanasController = gpioHermanasController;
    }

    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton = gpioHermanasController.provisionInput(doorButtonBottomGpioAddress);
            bottomButton.setShutdownOptions(true);
            bottomButton.addListener(new BottomButtonListener(this, servoMotorController));
        }
    }

    public synchronized void unprovisionButton() {
        resetBottomButtonState();
        if (bottomButton != null) {
            logger.info("unprovision door button on gpio instance.");
            bottomButton.removeAllListeners();
            gpioHermanasController.unprovisionPin(bottomButton);
            bottomButton = null;
        }
    }

    public void resetBottomButtonState() {
        bottomButtonHasBeenPressed = false;
    }

    public boolean isBottomButtonHasBeenPressed() {
        return bottomButtonHasBeenPressed;
    }

    void setBottomButtonHasBeenPressed(boolean bottomButtonHasBeenPressed) {
        this.bottomButtonHasBeenPressed = bottomButtonHasBeenPressed;
    }

    public synchronized boolean isBottomButtonPressed() {
        boolean isButtonNull = bottomButton == null;
        if (isButtonNull) {
            provisionButton();
        }
        boolean isHigh = bottomButton.isHigh();
        if (isButtonNull) {
            unprovisionButton();
        }
        return isHigh;
    }
}
