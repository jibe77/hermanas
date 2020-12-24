package org.jibe77.hermanas.controller.energy;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class WifiController {

    ProcessLauncher processLauncher;

    ConsumptionModeManager consumptionModeManager;

    DoorController doorController;

    @Value("${wifi.switch.enabled}")
    private boolean wifiSwitchEnabled;

    Logger logger = LoggerFactory.getLogger(WifiController.class);

    public WifiController(ProcessLauncher processLauncher, ConsumptionModeManager consumptionModeManager, DoorController doorController) {
        this.processLauncher = processLauncher;
        this.consumptionModeManager = consumptionModeManager;
        this.doorController = doorController;
    }

    @PostConstruct
    private synchronized void init() {
        if (wifiSwitchEnabled && consumptionModeManager.isEcoMode() && doorController.doorIsClosed()) {
            logger.info("Init Wifi controller in eco mode. Stopping wifi now.");
            turnOff();
        } else {
            logger.info("Init wifi controller.");
            turnOn();
        }
    }

    @PreDestroy
    private synchronized  void tearDown() {
        turnOn();
    }

    public synchronized boolean turnOn() {
        if (!wifiCardIsEnabled()) {
            try {
                logger.info("Turning on wifi on wlan0.");
                processLauncher.launch("/usr/sbin/rfkill", "unblock", "0");
                Process process = processLauncher.launch("/sbin/iwconfig", "wlan0", "txpower", "on");
                process.waitFor();
                wait(10000); // give 10 seconds to the system before using the connection.
                logger.info("Returned value {}.", process.exitValue());
                return process.exitValue() == 0;
            } catch (IOException e) {
                logger.error("Exception when turning on the wifi card : ", e);
                return false;
            } catch (InterruptedException e) {
                logger.error("Interrupted when turning on the wifi card : ", e);
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            return true;
        }
    }

    public synchronized boolean turnOff() {
        boolean isEnabled = wifiCardIsEnabled();
        logger.info("Turn off method is called (status enable : {}, switch enabled : {}.", isEnabled, wifiSwitchEnabled);
        if (wifiSwitchEnabled && isEnabled) {
            try {
                logger.info("Turning off wifi on wlan0.");
                Process process = processLauncher.launch("/sbin/iwconfig", "wlan0", "txpower", "off");
                process.waitFor();
                wait(10000); // give 10 seconds to the system before using the connection.
                logger.info("Returned value {}.", process.exitValue());
                return process.exitValue() == 0;
            } catch (IOException e) {
                logger.error("Exception when turning off the wifi card : ", e);
                return false;
            } catch (InterruptedException e) {
                logger.error("Interrupted when turning on the wifi card : ", e);
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            logger.info(
                    "turnoff method is not taken in account : switch is enabled : {} and wifi card is enabled : {}.",
                    wifiSwitchEnabled, isEnabled);
            return false;
        }
    }

    /**
     * Returns the status of the wifi card.
     * @return true if the wifi card is enabled.
     */
    public synchronized boolean wifiCardIsEnabled() {
        try {
            String returnedValue = processLauncher.launchAndReturnResult(
                    "/bin/cat",
                    "/sys/class/net/wlan0/carrier");
            logger.info("wifi status is {}.", returnedValue);
            boolean isEnabled = "1".equals(returnedValue);
            logger.info("wifi card is enabled : {}.", isEnabled);
            return isEnabled;
        } catch (IOException e) {
            logger.error("Error checking wlan0 status.");
            return false;
        }
    }
}
