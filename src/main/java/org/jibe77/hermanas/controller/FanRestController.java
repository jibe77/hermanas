package org.jibe77.hermanas.service;

import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.controller.fan.FanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fan")
public class FanRestController {

    FanService fanService;

    public FanRestController(FanService fanService) {
        this.fanService = fanService;
    }

    @GetMapping(value = "/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return fanService.switcher(param);
    }

    @GetMapping(value = "/status")
    public Status getStatus() {
        return fanService.getStatus();
    }
}
