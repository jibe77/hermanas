package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Component
public class ManageDoorClosingEvent {

    SunTimeManager sunTimeManager;

    DoorController doorController;

    CameraController cameraController;

    EmailService emailService;

    MessageSource messageSource;

    MusicController musicController;

    @Value("${email.notification.sunset.subject}")
    private String emailNotificationSunsetSubject;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    Logger logger = LoggerFactory.getLogger(ManageDoorClosingEvent.class);

    public ManageDoorClosingEvent(SunTimeManager sunTimeManager, DoorController doorController, CameraController cameraController, EmailService emailService, MessageSource messageSource, MusicController musicController) {
        this.sunTimeManager = sunTimeManager;
        this.doorController = doorController;
        this.cameraController = cameraController;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.musicController = musicController;
    }

    public void manageDoorClosingEvent(LocalDateTime currentTime) {
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
            if (playSongAtSunset) {
                musicController.playMusicRandomly();
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

    protected void setPlaySongAtSunset(boolean playSongAtSunset) {
        this.playSongAtSunset = playSongAtSunset;
    }
}
