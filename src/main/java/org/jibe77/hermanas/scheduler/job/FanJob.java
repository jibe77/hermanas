package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.fan.FanService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FanJob {

    private FanService fanService;

    private SunTimeUtils sunTimeUtils;

    private ConsumptionModeController consumptionModeController;

    private static final Logger logger = LoggerFactory.getLogger(FanJob.class);

    public FanJob(FanService fanService, SunTimeUtils sunTimeUtils, ConsumptionModeController consumptionModeController) {
        this.fanService = fanService;
        this.sunTimeUtils = sunTimeUtils;
        this.consumptionModeController = consumptionModeController;
    }

    @Scheduled(fixedDelayString = "${fan.scheduler.delay.in.milliseconds}")
    public void execute() {
        if (!consumptionModeController.isEcoMode(LocalDateTime.now()) && !sunTimeUtils.isDay()) {
            logger.info("fan scheduled job is switching on the fan.");
            fanService.switchOn();
        }
    }
}
