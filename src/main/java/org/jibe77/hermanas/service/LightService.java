package org.jibe77.hermanas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LightService {

    Logger logger = LoggerFactory.getLogger(LightService.class);

    @GetMapping("/light/on")
    public void on() {
        logger.info("light on not implemented yet.");
        // TODO ...
    }

    @GetMapping("/light/off")
    public void off() {
        logger.info("light off not implemented yet.");
        // TODO ...
    }
}
