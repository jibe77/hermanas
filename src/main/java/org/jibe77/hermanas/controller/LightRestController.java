package org.jibe77.hermanas.service;

import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.controller.light.LightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/light")
public class LightRestController {

    LightService lightService;

    public LightRestController(LightService lightService) {
        this.lightService = lightService;
    }

    @PostMapping(value = "/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return lightService.switcher(param);
    }

    @GetMapping(value = "/status")
    public Status getStatus() {
        return lightService.getStatus();
    }
}
