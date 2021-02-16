package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BirdhouseButtonListener implements GpioPinListenerDigital {

    BirdhouseButtonController birdhouseButtonController;
    LightController lightController;

    Logger logger = LoggerFactory.getLogger(BirdhouseButtonListener.class);

    public BirdhouseButtonListener(BirdhouseButtonController birdhouseButtonController, LightController lightController) {
        this.birdhouseButtonController = birdhouseButtonController;
        this.lightController = lightController;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh()) {
            logger.info("Birdhouse button is pressed.");
            if (StatusEnum.ON.equals(lightController.getStatus().getStatusEnum())) {
                birdhouseButtonController.setLightHasBeenSwitchedOnByBirdhouseDoor(false);
            } else {
                birdhouseButtonController.setLightHasBeenSwitchedOnByBirdhouseDoor(true);
                lightController.switchOn();
            }
        } else if (event.getState().isLow()) {
            logger.info("Birdhouse button is not pressed anymore.");
            if (birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor()) {
                lightController.switchOff();
            }
            birdhouseButtonController.setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        }
    }
}
