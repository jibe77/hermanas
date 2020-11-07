package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.fan.FanController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FanJob {

    private FanController fanController;

    Logger logger = LoggerFactory.getLogger(FanJob.class);

    public FanJob(FanController fanController) {
        this.fanController = fanController;
    }

    @Scheduled(fixedDelayString = "${fan.scheduler.delay.in.milliseconds}")
    public void execute() {
        logger.info("fan scheduled job is switching on the fan.");
        fanController.switchOn();
    }
}