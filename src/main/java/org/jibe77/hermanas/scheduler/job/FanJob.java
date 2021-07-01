package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FanJob {

    private FanController fanController;

    private SunTimeUtils sunTimeUtils;

    private ConsumptionModeController consumptionModeController;

    Logger logger = LoggerFactory.getLogger(FanJob.class);

    public FanJob(FanController fanController, SunTimeUtils sunTimeUtils, ConsumptionModeController consumptionModeController) {
        this.fanController = fanController;
        this.sunTimeUtils = sunTimeUtils;
        this.consumptionModeController = consumptionModeController;
    }

    @Scheduled(fixedDelayString = "${fan.scheduler.delay.in.milliseconds}")
    public void execute() {
        if (!consumptionModeController.isEcoMode(LocalDateTime.now()) && !sunTimeUtils.isDay()) {
            logger.info("fan scheduled job is switching on the fan.");
            fanController.switchOn();
        }
    }
}
