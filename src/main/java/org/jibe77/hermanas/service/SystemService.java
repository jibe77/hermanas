package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.system.SystemController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemService {

    SystemController systemController;

    public SystemService(SystemController systemController) {
        this.systemController = systemController;
    }

    @GetMapping(value = "/system/shutdown")
    public void shutdown() {
        systemController.shutdown();
    }

    @GetMapping(value = "/system/reboot")
    public void reboot() {
        systemController.reboot();
    }

    @GetMapping(value = "/system/version")
    public void version() {
        systemController.reboot();
    }
}
