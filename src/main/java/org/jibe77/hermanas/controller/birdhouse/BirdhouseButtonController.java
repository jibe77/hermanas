package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class BirdhouseButtonController {

    @Value("${birdhouse.button.gpio.address}")
    private int buttonGpioAddress;

    private DigitalInput button;

    private GpioHermanasController gpioHermanasController;

    private LightController lightController;

    Logger logger = LoggerFactory.getLogger(BirdhouseButtonController.class);

    private boolean lightHasBeenSwitchedOnByBirdhouseDoor;

    public BirdhouseButtonController(GpioHermanasController gpioHermanasController, LightController lightController) {
        this.gpioHermanasController = gpioHermanasController;
        this.lightController = lightController;
    }

    @PostConstruct
    synchronized void initButton() {
        logger.info("provision birdhouse button on gpio instance.");
        if (button == null) {
            button = gpioHermanasController.provisionInput("birdhouse_button", "Birdhouse button", buttonGpioAddress);
            button.addListener(event -> manageEvent(event));
        }
    }

    void manageEvent(DigitalStateChangeEvent event) {
        if (event.state().isHigh()) {
            logger.info("Birdhouse button is pressed.");
            if (StatusEnum.ON.equals(lightController.getStatus().getStatusEnum())) {
                setLightHasBeenSwitchedOnByBirdhouseDoor(false);
            } else {
                setLightHasBeenSwitchedOnByBirdhouseDoor(true);
                lightController.switchOn();
            }
        } else if (event.state().isLow()) {
            logger.info("Birdhouse button is not pressed anymore.");
            if (isLightHasBeenSwitchedOnByBirdhouseDoor()) {
                lightController.switchOff();
            }
            setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        }
    }

    void setLightHasBeenSwitchedOnByBirdhouseDoor(boolean lightHasBeenSwitchedOnByBirdhouseDoor) {
        this.lightHasBeenSwitchedOnByBirdhouseDoor = lightHasBeenSwitchedOnByBirdhouseDoor;
    }

    boolean isLightHasBeenSwitchedOnByBirdhouseDoor() {
        return this.lightHasBeenSwitchedOnByBirdhouseDoor;
    }

    void setButton(DigitalInput button) {
        this.button = button;
    }
}
