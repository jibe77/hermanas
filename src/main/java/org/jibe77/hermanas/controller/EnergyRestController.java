package org.jibe77.hermanas.service;

import org.jibe77.hermanas.service.energy.EnergyMode;
import org.jibe77.hermanas.service.energy.EnergyModeConfig;
import org.jibe77.hermanas.service.energy.EnergyModeEnum;
import org.jibe77.hermanas.controller.energy.WifiService;
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
public class EnergyRestController {

    WifiService wifiService;

    ConsumptionModeController consumptionModeController;

    private static final Logger logger = LoggerFactory.getLogger(EnergyRestController.class);

    public EnergyRestController(WifiService wifiService, ConsumptionModeController consumptionModeController) {
        this.wifiService = wifiService;
        this.consumptionModeController = consumptionModeController;
    }

    @GetMapping(value = "/energy/wifi/stopUntilNextDoorEvent")
    public boolean stopWifiUntilNextDoorEvent() {
        logger.info("The network wifi card is going to be disabled.");
        wifiService.turnOffAfter(3);
        return true;
    }

    @GetMapping(value = "/energy/wifi/wifiSwitchEnabled")
    public boolean wifiSwitchEnabled(boolean wifiSwitchEnabled) {
        wifiService.setWifiSwitchEnabled(wifiSwitchEnabled);
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
