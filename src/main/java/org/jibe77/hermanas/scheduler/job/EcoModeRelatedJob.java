package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.energy.WifiService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EcoModeRelatedJob {

    WifiService wifiService;

    ConsumptionModeController consumptionModeController;

    public EcoModeRelatedJob(WifiService wifiService, ConsumptionModeController consumptionModeController) {
        this.wifiService = wifiService;
        this.consumptionModeController = consumptionModeController;
    }

    @Scheduled(cron = "0 0 21 * * ?")
    void turnOffWifiInTheEveningInEcoMode() {
        if (consumptionModeController.isEcoMode(LocalDateTime.now()))
            wifiService.turnOff();
    }

}
