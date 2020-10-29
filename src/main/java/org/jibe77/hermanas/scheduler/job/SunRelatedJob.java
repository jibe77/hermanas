package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Component
public class SunRelatedJob {

    private SunTimeManager sunTimeManager;

    private CameraController cameraController;

    private LightController lightController;

    private DoorController doorController;

    private EmailService emailService;

    private MusicController musicController;

    private MessageSource messageSource;

    @Value("${email.notification.sunset.subject}")
    private String emailNotificationSunsetSubject;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    public SunRelatedJob(SunTimeManager sunTimeManager, CameraController cameraController,
                         LightController lightController, EmailService emailService, DoorController doorController,
                         MusicController musicController, MessageSource messageSource) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.lightController = lightController;
        this.emailService = emailService;
        this.doorController = doorController;
        this.musicController = musicController;
        this.messageSource = messageSource;
    }

    Logger logger = LoggerFactory.getLogger(SunRelatedJob.class);

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    void execute() {
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
        }
    }

    private void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            logger.info("light switching on event is starting now.");
            lightController.switchOn();
            if (playSongAtSunset) {
                musicController.playMusicRandomly();
            }
            if (!doorController.doorIsOpened()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorController.openDoor(false);
            }
            sunTimeManager.reloadLightOnTime();
        }
    }

    private void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            logger.info("door opening event is starting now.");
            cameraController.takePictureNoException(true);
            doorController.openDoor(false);
            if (cocoricoAtSunriseEnabled) {
                musicController.cocorico();
            }
            cameraController.takePictureNoException(true);
            sunTimeManager.reloadDoorOpeningTime();
        }
    }

    private void manageDoorClosingEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (!doorController.doorIsClosed()) {
                try {
                    logger.info("start door closing job at sunset.");
                    logger.info("take picture before closing door.");
                    cameraController.takePictureNoException(true);
                    logger.info("close door");
                    doorController.closeDoorWithBottormButtonManagement(false);
                    logger.info("take picture once the door is closed and send it by email.");
                    notification(
                            messageSource.getMessage("event.closing.mail.with_picture.title", null, Locale.getDefault()),
                            messageSource.getMessage("event.closing.mail.without_picture.title", null,
                                    Locale.getDefault()));
                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                    notification(
                        messageSource.getMessage("event.closing_with_problem.mail.with_picture.title",
                                null, Locale.getDefault()),
                        messageSource.getMessage("event.closing_with_problem.mail.without_picture.title",
                                null, Locale.getDefault()));
                }
            } else {
                logger.info("door has already been closed before, nothing to do in this event.");
            }
            sunTimeManager.reloadDoorClosingTime();
        }
    }

    private void notification(String textWithPicture, String textIfNoPicture) {
        Optional<File> picWithClosedDoor = cameraController.takePictureNoException(true);
        if (picWithClosedDoor.isPresent()) {
            emailService.sendMailWithAttachment(emailNotificationSunsetSubject,
                    textWithPicture, picWithClosedDoor.get());
        } else {
            emailService.sendMail(emailNotificationSunsetSubject,
                    textIfNoPicture);
        }
    }
}