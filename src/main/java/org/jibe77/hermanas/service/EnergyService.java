package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class EnergyService {

    WifiController wifiController;

    Logger logger = LoggerFactory.getLogger(EnergyService.class);

    public EnergyService(WifiController wifiController) {
        this.wifiController = wifiController;
    }

    @GetMapping(value = "/energy/wifi/stopUntilNextDoorEvent")
    public boolean stopWifiUntilNextDoorEvent() {
        logger.info("The network wifi card is going to be disabled.");
        new Thread(() -> {
            try {
                logger.info("Wifi card will be disabled in 3 seconds ...");
                TimeUnit.SECONDS.sleep(3);
                logger.info("Wifi card will be disabled now ...");
                wifiController.turnOff();
                logger.info("... done, the wifi card is disabled.");
            } catch (InterruptedException e) {
                logger.error("Interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        ).start();
        return true;
    }
}
