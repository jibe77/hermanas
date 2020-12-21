package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EcoModeRelatedJob {

    WifiController wifiController;

    ConsumptionModeManager consumptionModeManager;

    public EcoModeRelatedJob(WifiController wifiController, ConsumptionModeManager consumptionModeManager) {
        this.wifiController = wifiController;
        this.consumptionModeManager = consumptionModeManager;
    }

    @Scheduled(cron = "0 0 7 * * ?")
    void turnOnWifiInTheMorning() {
        wifiController.turnOn();
    }

    @Scheduled(cron = "0 0 21 * * ?")
    void turnOffWifiInTheEveningInEcoMode() {
        if (consumptionModeManager.isEcoMode())
            wifiController.turnOff();
    }

}
