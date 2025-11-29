package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.system.SystemService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemRestController {

    SystemService systemService;

    public SystemRestController(SystemService systemService) {
        this.systemService = systemService;
    }

    @PostMapping(value = "/system/shutdown")
    public void shutdown() {
        systemService.shutdown();
    }

    @PostMapping(value = "/system/reboot")
    public void reboot() {
        systemService.reboot();
    }
}
