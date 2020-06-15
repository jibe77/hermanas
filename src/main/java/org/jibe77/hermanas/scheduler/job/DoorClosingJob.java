package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.service.DoorService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DoorClosingJob implements Job {

    @Autowired
    /**
     * Quartz is instantiating the job with default constructor,
     * so it's not possible to inject beans with constructor.
     */
    public DoorService doorService;

    Logger logger = LoggerFactory.getLogger(DoorClosingJob.class);

    public void execute(JobExecutionContext context) {
        try {
            logger.info("start door closing job at sunset.");
            doorService.close();
        } catch (DoorNotClosedCorrectlyException e) {
            logger.error("Didn't close the door correctly.");
        }
    }
}