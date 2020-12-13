package org.jibe77.hermanas.controller.door.upbutton;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpButtonListener implements GpioPinListenerDigital {

    UpButtonController upButtonController;
    ServoMotorController servoMotorController;

    Logger logger = LoggerFactory.getLogger(UpButtonListener.class);

    public UpButtonListener(UpButtonController upButtonController, ServoMotorController servoMotorController) {
        this.upButtonController = upButtonController;
        this.servoMotorController = servoMotorController;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh()) {
            logger.info("Door has reached the up, stop servomotor now !");
            upButtonController.setUpButtonHasBeenPressed(true);
            servoMotorController.stop();
        } else if (event.getState().isLow()) {
            logger.info("Up button is not pressed anymore.");
        }
    }
}
