package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.fan.FanController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FanService {

    FanController fanController;

    public FanService(FanController fanController) {
        this.fanController = fanController;
    }

    Logger logger = LoggerFactory.getLogger(FanService.class);

    @CrossOrigin
    @GetMapping(value = "/fan/on")
    public void switchOn() {
        logger.info("Rest service is switching the fan on.");
        fanController.switchOn();
    }

    @CrossOrigin
    @GetMapping(value = "/fan/off")
    public void switchOff() {
        logger.info("Rest service is switching the fan off.");
        fanController.switchOff();
    }

    @CrossOrigin
    @GetMapping(value = "/fan/status")
    public boolean isSwitchedOn() {
        return fanController.isSwitchedOn();
    }
}
