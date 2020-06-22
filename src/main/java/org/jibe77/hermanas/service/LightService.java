package org.jibe77.hermanas.service;

import org.jibe77.hermanas.gpio.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LightService {

    LightController lightController;

    public LightService(LightController lightController) {
        this.lightController = lightController;
    }

    Logger logger = LoggerFactory.getLogger(LightService.class);

    @GetMapping(value = "/light/on")
    public void switchOn() {
        lightController.switchOn();
    }

    @GetMapping(value = "/light/off")
    public void switchOff() {
        lightController.switchOff();
    }
}
