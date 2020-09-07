package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
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

    private MusicController musicController;

    @Value("${email.notification.sunset.subject}")
    private String emailNotificationSunsetSubject;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    public SunRelatedJob(SunTimeManager sunTimeManager, CameraController cameraController, LightController lightController, DoorService doorService, EmailService emailService, DoorController doorController, MusicController musicController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.lightController = lightController;
        this.doorService = doorService;
        this.emailService = emailService;
        this.doorController = doorController;
        this.musicController = musicController;
    }

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    public void execute() {
        LocalDateTime currentTime = LocalDateTime.now();
        manageDoorClosingEvent(currentTime);
        manageDoorOpeningEvent(currentTime);
        manageLightSwitchingOnEvent(currentTime);
        manageLightSwitchingOffEvent(currentTime);
    }

    private void manageLightSwitchingOffEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOffTime())) {
            logger.info("light switching off event is starting now.");
            lightController.switchOff();
            sunTimeManager.reloadLightOffTime();
            musicController.stop();
        }
    }

    private void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            logger.info("light switching on event is starting now.");
            lightController.switchOn();
            if (playSongAtSunset) {
                musicController.playMusicRandomly();
            }
            if (doorController.doorIsClosed()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorController.openDoor();
            }
            sunTimeManager.reloadLightOnTime();
        }
    }

    private void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            logger.info("door opening event is starting now.");
            cameraController.takePictureNoException();
            doorService.open();
            if (cocoricoAtSunriseEnabled) {
                musicController.cocorico();
            }
            cameraController.takePictureNoException();
            sunTimeManager.reloadDoorOpeningTime();
        }
    }

    private void manageDoorClosingEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (doorController.doorIsOpened()) {
                try {
                    logger.info("start door closing job at sunset.");
                    logger.info("take picture before closing door.");
                    cameraController.takePictureNoException();
                    logger.info("close door");
                    doorService.close();
                    logger.info("take picture once the door is closed and send it by email.");
                    Optional<File> picWithClosedDoor = cameraController.takePictureNoException();
                    if (picWithClosedDoor.isPresent()) {
                        emailService.sendMailWithAttachment("Sunset notif: door is closed",
                                "Here is a picture inside the chicken coop :", picWithClosedDoor.get());
                    } else {
                        emailService.sendMail("Sunset notif: door is closed",
                                "The picture inside the chicken coop is not available (camera problem ?).");
                    }
                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                }
                cameraController.takePictureNoException();
            } else {
                logger.info("door has already been closed before, nothing to do in this event.");
            }
            sunTimeManager.reloadDoorClosingTime();
        }
    }
}