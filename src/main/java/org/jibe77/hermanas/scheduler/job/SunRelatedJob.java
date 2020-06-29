package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.camera.CameraController;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.light.LightController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.DoorService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunRelatedJob {

    private SunTimeManager sunTimeManager;

    private CameraController cameraController;

    private LightController lightController;

    private DoorService doorService;

    public SunRelatedJob(SunTimeManager sunTimeManager, CameraController cameraController, LightController lightController, DoorService doorService) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.lightController = lightController;
        this.doorService = doorService;
    }

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    public void execute() {
        LocalDateTime currentTime = LocalDateTime.now();
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            try {
                logger.info("start door closing job at sunset.");
                cameraController.takePictureNoException();
                doorService.close();
            } catch (DoorNotClosedCorrectlyException e) {
                logger.error("Didn't close the door correctly.");
            }
            cameraController.takePictureNoException();
            sunTimeManager.reloadDoorClosingTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            cameraController.takePictureNoException();
            doorService.open();
            cameraController.takePictureNoException();
            sunTimeManager.reloadDoorOpeningTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            lightController.switchOn();
            sunTimeManager.reloadLightOnTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextLightOffTime())) {
            lightController.switchOff();
            sunTimeManager.reloadLightOffTime();
        }

    }
}