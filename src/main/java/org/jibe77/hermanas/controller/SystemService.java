package org.jibe77.hermanas.controller;

import org.jibe77.hermanas.service.system.SystemController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemService {

    SystemController systemController;

    public SystemService(SystemController systemController) {
        this.systemController = systemController;
    }

    @PostMapping(value = "/system/shutdown")
    public void shutdown() {
        systemController.shutdown();
    }

    @PostMapping(value = "/system/reboot")
    public void reboot() {
        systemController.reboot();
    }
}
