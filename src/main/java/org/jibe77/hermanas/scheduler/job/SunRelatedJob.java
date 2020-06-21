package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.camera.CameraController;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.scheduler.SunTimeService;
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
    SunTimeService sunTimeService;

    @Autowired
    CameraController cameraController;

    @Autowired
    /**
     * Quartz is instantiating the job with default constructor,
     * so it's not possible to inject beans with constructor.
     */
    public DoorService doorService;

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    public void execute(JobExecutionContext context) {
        LocalDateTime currentTime = LocalDateTime.now();
        if (currentTime.isAfter(sunTimeService.getNextDoorClosingTime())) {
            try {
                logger.info("start door closing job at sunset.");
                cameraController.takePictureNoException();
                doorService.close();
            } catch (DoorNotClosedCorrectlyException e) {
                logger.error("Didn't close the door correctly.");
            }
            cameraController.takePictureNoException();
            sunTimeService.reloadDoorClosingTime();
        } else if (currentTime.isAfter(sunTimeService.getNextDoorOpeningTime())) {
            cameraController.takePictureNoException();
            doorService.open();
            cameraController.takePictureNoException();
            sunTimeService.reloadDoorOpeningTime();
        } else if (currentTime.isAfter(sunTimeService.getNextLightOnTime())) {
            // TODO ...
            sunTimeService.reloadLightOnTime();
        } else if (currentTime.isAfter(sunTimeService.getNextLightOffTime())) {
            // TODO ...
            sunTimeService.reloadLightOffTime();
        }

    }
}