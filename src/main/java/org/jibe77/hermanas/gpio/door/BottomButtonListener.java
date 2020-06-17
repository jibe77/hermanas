package org.jibe77.hermanas.gpio.door;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottomButtonListener implements GpioPinListenerDigital {

    ServoMotor servoMotor;

    Logger logger = LoggerFactory.getLogger(BottomButtonListener.class);

    public BottomButtonListener(ServoMotor servoMotor) {
        this.servoMotor = servoMotor;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        logger.info("Door has reached the bottom, stop servomotor now !");
        servoMotor.stopAtBottom();
    }
}
