package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnergyService {

    WifiController wifiController;

    public EnergyService(WifiController wifiController) {
        this.wifiController = wifiController;
    }

    @GetMapping(value = "/energy/wifi/stopUntilNextDoorEvent")
    public boolean stopWifiUntilNextDoorEvent() {
        wifiController.turnOff();
        return true;
    }
}
