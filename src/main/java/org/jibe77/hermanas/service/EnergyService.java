package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.energy.EnergyMode;
import org.jibe77.hermanas.controller.energy.EnergyModeConfig;
import org.jibe77.hermanas.controller.energy.EnergyModeEnum;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return consumptionModeController.getCurrentEnergyMode();
    }

    @GetMapping(value = "/energy/dateRange")
    public EnergyMode getEnergyDateRange(int daysAroundWinterSolstice, int daysAroundSummerSolstice) {
        return consumptionModeController.getCurrentEnergyMode(LocalDateTime.now(), daysAroundWinterSolstice, daysAroundSummerSolstice);
    }

    @GetMapping(value = "/energy/currentConfigMode")
    public EnergyModeConfig getCurrentConfigMode() {
        return consumptionModeController.getCurrentConfigMode();
    }

    @GetMapping(value = "/energy/configMode")
    public EnergyModeConfig getEnergyConfigMode(String energyMode) {
        return consumptionModeController.getEnergyModeConfig(energyMode);
    }

    @PutMapping(value = "/energy/updateMode")
    public void updateEnergyConfigMode(EnergyModeConfig energyModeConfig) {
        consumptionModeController.updateEnergyModeConfig(energyModeConfig);
    }
}
