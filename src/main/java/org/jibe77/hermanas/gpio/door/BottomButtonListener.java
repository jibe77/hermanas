package org.jibe77.hermanas.gpio.door;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottomButtonListener implements GpioPinListenerDigital {

    BottomButtonController bottomButtonController;
    ServoMotorController servoMotorController;

    Logger logger = LoggerFactory.getLogger(BottomButtonListener.class);

    public BottomButtonListener(BottomButtonController bottomButtonController, ServoMotorController servoMotorController) {
        this.bottomButtonController = bottomButtonController;
        this.servoMotorController = servoMotorController;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh()) {
            logger.info("Door has reached the bottom, stop servomotor now !");
            bottomButtonController.setBottomButtonPressed(true);
            servoMotorController.stop();
        } else if (event.getState().isLow()) {
            logger.info("Bottom button is not pressed anymore.");
            bottomButtonController.setBottomButtonPressed(false);
        }
    }
}
