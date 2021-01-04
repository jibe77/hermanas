package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FanJob {

    private FanController fanController;

    private SunTimeUtils sunTimeUtils;

    private ConsumptionModeManager consumptionModeManager;

    Logger logger = LoggerFactory.getLogger(FanJob.class);

    public FanJob(FanController fanController, SunTimeUtils sunTimeUtils, ConsumptionModeManager consumptionModeManager) {
        this.fanController = fanController;
        this.sunTimeUtils = sunTimeUtils;
        this.consumptionModeManager = consumptionModeManager;
    }

    @Scheduled(fixedDelayString = "${fan.scheduler.delay.in.milliseconds}")
    public void execute() {
        if (!consumptionModeManager.isEcoMode() && !sunTimeUtils.isDay()) {
            logger.info("fan scheduled job is switching on the fan.");
            fanController.switchOn();
        }
    }
}
