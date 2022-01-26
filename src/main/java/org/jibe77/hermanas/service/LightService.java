package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.light.LightController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LightService {

    LightController lightController;

    public LightService(LightController lightController) {
        this.lightController = lightController;
    }

    @GetMapping(value = "/light/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return lightController.switcher(param);
    }

    @GetMapping(value = "/light/status")
    public Status getStatus() {
        return lightController.getStatus();
    }
}
