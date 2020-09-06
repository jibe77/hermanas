package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class BirdhouseButtonController {

    @Value("${birdhouse.button.gpio.address}")
    private int buttonGpioAddress;

    private GpioPinDigitalInput button;

    private GpioHermanasController gpioHermanasController;

    private LightController lightController;

    Logger logger = LoggerFactory.getLogger(BirdhouseButtonController.class);

    private boolean lightHasBeenSwitchedOnByBirdhouseDoor;

    public BirdhouseButtonController(GpioHermanasController gpioHermanasController, LightController lightController) {
        this.gpioHermanasController = gpioHermanasController;
        this.lightController = lightController;
    }

    @PostConstruct
    void init() {
        logger.info("Init Birdhouse controller at startup.");
        provisionButton();
    }

    private synchronized void provisionButton() {
        if (button == null) {
            logger.info("provision birdhouse button on gpio instance.");
            button = gpioHermanasController.provisionInput(buttonGpioAddress);
            button.setShutdownOptions(true);
            button.addListener(new BirdhouseButtonListener(this, lightController));
        }
    }

    @PreDestroy
    void tearDown() {
        logger.info("Tear down Birdhouse controller at shutdown.");
        unprovisionButton();
    }

    private synchronized void unprovisionButton() {
        if (button != null) {
            logger.info("unprovision door button on gpio instance.");
            button.removeAllListeners();
            gpioHermanasController.unprovisionPin(button);
            button = null;
        }
    }

    void setLightHasBeenSwitchedOnByBirdhouseDoor(boolean lightHasBeenSwitchedOnByBirdhouseDoor) {
        this.lightHasBeenSwitchedOnByBirdhouseDoor = lightHasBeenSwitchedOnByBirdhouseDoor;
    }

    boolean isLightHasBeenSwitchedOnByBirdhouseDoor() {
        return this.lightHasBeenSwitchedOnByBirdhouseDoor;
    }

    void setButton(GpioPinDigitalInput button) {
        this.button = button;
    }
}
