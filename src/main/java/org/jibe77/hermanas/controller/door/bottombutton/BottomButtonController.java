package org.jibe77.hermanas.controller.door.bottombutton;

import com.pi4j.io.gpio.digital.DigitalInput;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BottomButtonController {

    final GpioHermanasController gpioHermanasController;

    final ServoMotorController servoMotorController;

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomGpioAddress;

    private DigitalInput bottomButton;


    boolean bottomButtonHasBeenPressed = false;

    Logger logger = LoggerFactory.getLogger(BottomButtonController.class);

    public BottomButtonController(GpioHermanasController gpioHermanasController, ServoMotorController servoMotorController) {
        this.gpioHermanasController = gpioHermanasController;
        this.servoMotorController = servoMotorController;
    }

    @PostConstruct
    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton = gpioHermanasController.provisionInput(
                    "door_bottom_button",
                    "Door bottom button",
                    doorButtonBottomGpioAddress);
            bottomButton.addListener(event -> {
                if (event.state().isHigh()) {
                    logger.info("Door has reached the bottom, stop servomotor now !");
                    this.bottomButtonHasBeenPressed = true;
                    servoMotorController.stop();
                } else if (event.state().isLow()) {
                    logger.info("Bottom button is not pressed anymore.");
                }
            });
        }
    }

    public void resetBottomButtonState() {
        bottomButtonHasBeenPressed = false;
    }

    public boolean isBottomButtonHasBeenPressed() {
        return bottomButtonHasBeenPressed;
    }

    public synchronized boolean isBottomButtonPressed() {
        return bottomButton.isHigh();
    }
}
