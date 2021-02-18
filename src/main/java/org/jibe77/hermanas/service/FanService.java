package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.fan.FanController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FanService {

    FanController fanController;

    public FanService(FanController fanController) {
        this.fanController = fanController;
    }

    @GetMapping(value = "/fan/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return fanController.switcher(param);
    }

    @GetMapping(value = "/fan/status")
    public Status getStatus() {
        return fanController.getStatus();
    }
}
