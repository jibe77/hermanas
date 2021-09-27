package org.jibe77.hermanas.controller.door.upbutton;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UpButtonController {

    final ServoMotorController servoMotorController;

    final GpioHermanasController gpioHermanasController;

    @Value("${door.button.up.gpio.address}")
    private int doorButtonUpGpioAddress;

    private DigitalInput upButton;

    boolean upButtonHasBeenPressed = false;

    Logger logger = LoggerFactory.getLogger(UpButtonController.class);

    public UpButtonController(ServoMotorController servoMotorController, GpioHermanasController gpioHermanasController) {
        this.servoMotorController = servoMotorController;
        this.gpioHermanasController = gpioHermanasController;
    }

    @PostConstruct
    public synchronized void provisionButton() {
        logger.info("provision door button on gpio instance.");
        if (upButton == null) {
            upButton = gpioHermanasController.provisionInput(
                    "door_up_button", "Door up button", doorButtonUpGpioAddress);
            upButton.addListener(event -> manageEvent(event));
        }
    }

    private void manageEvent(DigitalStateChangeEvent event) {
        if (event.state().isHigh()) {
            logger.info("Door has reached the up, stop servomotor now !");
            this.setUpButtonHasBeenPressed(true);
            servoMotorController.stop();
        } else if (event.state().isLow()) {
            logger.info("Up button is not pressed anymore.");
        }
    }

    public synchronized boolean isUpButtonPressed() {
        return upButton.isHigh();
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
