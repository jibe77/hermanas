package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LightService {

    LightController lightController;

    public LightService(LightController lightController) {
        this.lightController = lightController;
    }

    Logger logger = LoggerFactory.getLogger(LightService.class);

    @GetMapping(value = "/light/on", produces = "application/json")
    @Secured("USER")
    public void switchOn() {
        logger.info("Rest service is switching the light on.");
        lightController.switchOn();
    }

    @GetMapping(value = "/light/off", produces = "application/json")
    @Secured("USER")
    public void switchOff() {
        logger.info("Rest service is switching the light off.");
        lightController.switchOff();
    }

    @GetMapping(value = "/light/isSwitchedOn")
    @Secured({"DEMO", "USER"})
    public boolean isSwitchedOn() {
        return lightController.isSwitchedOn();
    }
}
