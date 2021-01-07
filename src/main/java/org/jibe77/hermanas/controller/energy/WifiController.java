package org.jibe77.hermanas.controller.energy;

import org.jibe77.hermanas.client.email.EmailService;
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

    EmailService emailService;

    @Value("${wifi.switch.enabled}")
    private boolean wifiSwitchEnabled;

    Logger logger = LoggerFactory.getLogger(WifiController.class);

    public WifiController(ProcessLauncher processLauncher, ConsumptionModeManager consumptionModeManager, DoorController doorController, EmailService emailService) {
        this.processLauncher = processLauncher;
        this.consumptionModeManager = consumptionModeManager;
        this.doorController = doorController;
        this.emailService = emailService;
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
                TimeUnit.SECONDS.sleep(10); // give 10 seconds to the system before using the connection.
                emailService.processSendingQueue();
                return process.exitValue() == 0 || process.exitValue() == 249;
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
                int retry = 0;
                while (!emailService.isSendingQueueEmpty() && retry < 3) {
                    int waitingPeriod = 100;
                    logger.info("Email sending queue is not empty, waiting {} second ...", waitingPeriod);
                    TimeUnit.SECONDS.sleep(waitingPeriod); // give 10 seconds to the system before using the connection.
                    retry++;
                }
                logger.info("Turning off wifi on wlan0.");
                Process process = processLauncher.launch("/sbin/iwconfig", "wlan0", "txpower", "off");
                process.waitFor();
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
            boolean isEnabled = "1".equals(returnedValue);
            logger.info("wifi card is enabled : {}.", isEnabled);
            return isEnabled;
        } catch (IOException e) {
            logger.error("Error checking wlan0 status.");
            return false;
        }
    }

    /**
     * Turns off the wifi card asynchronously after the specified period of time.
     * @param seconds period of time (in seconds)
     */
    public void turnOffAfter(int seconds) {
        new Thread(() -> {
            try {
                logger.info("Wifi card will be disabled in {} seconds ...", seconds);
                TimeUnit.SECONDS.sleep(seconds);
                logger.info("Wifi card will be disabled now ...");
                turnOff();
                logger.info("... done, the wifi card is disabled.");
            } catch (InterruptedException e) {
                logger.error("Interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        ).start();
    }

    public void setWifiSwitchEnabled(boolean wifiSwitchEnabled) {
        this.wifiSwitchEnabled = wifiSwitchEnabled;
        if (!wifiSwitchEnabled) {
            turnOffAfter(3);
        }
    }
}
