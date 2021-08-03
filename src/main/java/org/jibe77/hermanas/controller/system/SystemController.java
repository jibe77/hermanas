package org.jibe77.hermanas.controller.system;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SystemController {

    ProcessLauncher processLauncher;

    Logger logger = LoggerFactory.getLogger(SystemController.class);

    public SystemController(ProcessLauncher processLauncher) {
        this.processLauncher = processLauncher;
    }

    public void shutdown() {
        try {
            logger.info("Shutting down the system now.");
            processLauncher.launch("shutdown", "-h", "now");
        } catch (IOException e) {
            logger.error("Can't shutdown now.", e);
        }
    }

    public void reboot() {
        try {
            logger.info("Rebooting the system now.");
            processLauncher.launch("reboot");
        } catch (IOException e) {
            logger.error("Can't reboot now.", e);
        }
    }
}
