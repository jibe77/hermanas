package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.jms.EmailService;
import org.jibe77.hermanas.gpio.camera.CameraController;
import org.jibe77.hermanas.gpio.door.DoorController;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.light.LightController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.DoorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class SunRelatedJob {

    private SunTimeManager sunTimeManager;

    private CameraController cameraController;

    private LightController lightController;

    private DoorService doorService;

    private DoorController doorController;

    private EmailService emailService;

    @Value("${email.notification.sunset.subject}")
    private String emailNotificationSunsetSubject;

    @Value("${suntime.scheduler.door.close.time_after_sunset}")
    private long doorCloseTimeAfterSunset;

    @Value("${suntime.scheduler.light.on.time_before_sunset}")
    private long lightOnTimeBeforeSunset;

    public SunRelatedJob(SunTimeManager sunTimeManager, CameraController cameraController, LightController lightController, DoorService doorService, EmailService emailService, DoorController doorController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.lightController = lightController;
        this.doorService = doorService;
        this.emailService = emailService;
        this.doorController = doorController;
    }

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    public void execute() {
        LocalDateTime currentTime = LocalDateTime.now();
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (doorIsOpened()) {
                try {
                    logger.info("start door closing job at sunset.");
                    logger.info("take picture before closing door.");
                    cameraController.takePictureNoException();
                    logger.info("close door");
                    doorService.close();
                    logger.info("take picture once the door is closed and send it by email.");
                    Optional<File> picWithClosedDoor = cameraController.takePictureNoException();
                    emailService.sendPicture(emailNotificationSunsetSubject, picWithClosedDoor);
                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                }
                cameraController.takePictureNoException();
            } else {
                logger.info("door has already been closed before, nothing to do in this event.");
            }
            sunTimeManager.reloadDoorClosingTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            logger.info("door opening event is starting now.");
            cameraController.takePictureNoException();
            doorService.open();
            cameraController.takePictureNoException();
            sunTimeManager.reloadDoorOpeningTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            logger.info("light switching on event is starting now.");
            lightController.switchOn();
            if (doorIsClosed()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorController.openDoor();
            }
            sunTimeManager.reloadLightOnTime();
        } else if (currentTime.isAfter(sunTimeManager.getNextLightOffTime())) {
            logger.info("light switching off event is starting now.");
            lightController.switchOff();
            sunTimeManager.reloadLightOffTime();
        }
    }

    /**
     * Tells if the door is opened, or probably opened.
     * @return true if the opening time is after the last closing time.
     *          true if the opening or closing time is unknown.
     */
    private boolean doorIsOpened() {
        Optional<LocalDateTime> lastClosingTime =  doorController.getLastClosingTime();
        Optional<LocalDateTime> lastOpeningTime = doorController.getLastOpeningTime();
        if (lastClosingTime.isPresent() && lastOpeningTime.isPresent()) {
            return lastOpeningTime.get().isAfter(lastClosingTime.get());
        } else {
            return true;
        }
    }

    /**
     * Tells if the door is closed, or probably closed.
     * @return true if the closing time is after the last opening time.
     *          true if the opening or closing time is unknown.
     */
    private boolean doorIsClosed() {
        Optional<LocalDateTime> lastClosingTime =  doorController.getLastClosingTime();
        Optional<LocalDateTime> lastOpeningTime = doorController.getLastOpeningTime();
        if (lastClosingTime.isPresent() && lastOpeningTime.isPresent()) {
            return lastClosingTime.get().isAfter(lastOpeningTime.get());
        } else {
            return true;
        }
    }
}