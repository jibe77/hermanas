package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.fan.FanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FanRestController {

    FanService fanService;

    public FanRestController(FanService fanService) {
        this.fanService = fanService;
    }

    @GetMapping(value = "/fan/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return fanService.switcher(param);
    }

    @GetMapping(value = "/fan/status")
    public Status getStatus() {
        return fanService.getStatus();
    }
}
