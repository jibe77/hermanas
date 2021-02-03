package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
        wifiController.turnOffAfter(3);
        return true;
    }

    @GetMapping(value = "/energy/wifi/wifiSwitchEnabled")
    public boolean wifiSwitchEnabled(boolean wifiSwitchEnabled) {
        wifiController.setWifiSwitchEnabled(wifiSwitchEnabled);
        return true;
    }
}
