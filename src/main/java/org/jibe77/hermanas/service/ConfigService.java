package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.config.ConfigController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigService {

    ConfigController configController;

    public ConfigService(ConfigController configController) {
        this.configController = configController;
    }

    @CrossOrigin
    @GetMapping(value = "/config/shutdown")
    public void shutdown() {
        configController.shutdown();
    }

    @CrossOrigin
    @GetMapping(value = "/config/reboot")
    public void reboot() {
        configController.reboot();
    }
}
