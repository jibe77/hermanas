package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EcoModeRelatedJob {

    WifiController wifiController;

    ConsumptionModeController consumptionModeController;

    public EcoModeRelatedJob(WifiController wifiController, ConsumptionModeController consumptionModeController) {
        this.wifiController = wifiController;
        this.consumptionModeController = consumptionModeController;
    }

    @Scheduled(cron = "0 0 21 * * ?")
    void turnOffWifiInTheEveningInEcoMode() {
        if (consumptionModeController.isEcoMode(LocalDateTime.now()))
            wifiController.turnOff();
    }

}
