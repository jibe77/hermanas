package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.energy.EnergyMode;
import org.jibe77.hermanas.controller.energy.EnergyModeConfig;
import org.jibe77.hermanas.controller.energy.EnergyModeEnum;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class EnergyService {

    WifiController wifiController;

    ConsumptionModeController consumptionModeController;

    Logger logger = LoggerFactory.getLogger(EnergyService.class);

    public EnergyService(WifiController wifiController, ConsumptionModeController consumptionModeController) {
        this.wifiController = wifiController;
        this.consumptionModeController = consumptionModeController;
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

    @GetMapping(value = "/energy/currentMode")
    public EnergyMode getEnergyMode() {
        return consumptionModeController.getCurrentEnergyMode(LocalDateTime.now());
    }

    @GetMapping(value = "/energy/configMode")
    public EnergyModeConfig getEnergyConfigMode(EnergyModeEnum energyModeEnum) {
        return consumptionModeController.getEnergyModeConfig(energyModeEnum);
    }

}
