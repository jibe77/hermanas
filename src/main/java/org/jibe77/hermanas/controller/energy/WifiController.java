package org.jibe77.hermanas.controller.energy;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class WifiController {

    ProcessLauncher processLauncher;

    ConsumptionModeManager consumptionModeManager;

    Logger logger = LoggerFactory.getLogger(WifiController.class);

    public WifiController(ProcessLauncher processLauncher, ConsumptionModeManager consumptionModeManager) {
        this.processLauncher = processLauncher;
        this.consumptionModeManager = consumptionModeManager;
    }

    @PostConstruct
    private void init() {
        if (consumptionModeManager.isEcoMode()) {
            logger.info("Init Wifi controller in eco mode. Stopping wifi now.");
            turnOff();
        }
    }

    public void turnOn() {
        try {
            logger.info("Turning on wifi on wlan0.");
            processLauncher.launch("/usr/sbin/rfkill", "unblock", "0");
            processLauncher.launch("/sbin/iwconfig", "wlan0", "txpower", "on");
        } catch (IOException e) {
            logger.error("Exception when turning on the wifi card : ", e);
        }
    }

    public boolean turnOff() {
        try {
            logger.info("Turning off wifi on wlan0.");
            processLauncher.launch("/sbin/iwconfig", "wlan0", "txpower", "off");
            return true;
        } catch (IOException e) {
            logger.error("Exception when turning off the wifi card : ", e);
            return false;
        }
    }
}
