package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.scheduler.SunHourService;
import org.jibe77.hermanas.service.DoorService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunRelatedJob implements Job {

    @Autowired
    SunHourService sunHourService;

    @Autowired
    /**
     * Quartz is instantiating the job with default constructor,
     * so it's not possible to inject beans with constructor.
     */
    public DoorService doorService;

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    public void execute(JobExecutionContext context) {
        LocalDateTime currentTime = LocalDateTime.now();
        if (currentTime.isAfter(sunHourService.getNextDoorClosingTime())) {
            try {
                logger.info("start door closing job at sunset.");
                doorService.close();
            } catch (DoorNotClosedCorrectlyException e) {
                logger.error("Didn't close the door correctly.");
            }
            sunHourService.reloadDoorClosingTime();
        } else if (currentTime.isAfter(sunHourService.getNextDoorOpeningTime())) {
            doorService.open();
            sunHourService.reloadDoorOpeningTime();
        } else if (currentTime.isAfter(sunHourService.getNextLightOnTime())) {
            // TODO ...
            sunHourService.reloadLightOnTime();
        } else if (currentTime.isAfter(sunHourService.getNextLightOffTime())) {
            // TODO ...
            sunHourService.reloadLightOffTime();
        }

    }
}