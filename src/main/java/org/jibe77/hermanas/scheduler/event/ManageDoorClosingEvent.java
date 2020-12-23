package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.controller.energy.WifiController;
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

    WifiController wifiController;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    Logger logger = LoggerFactory.getLogger(ManageDoorClosingEvent.class);

    public ManageDoorClosingEvent(SunTimeManager sunTimeManager, DoorController doorController, CameraController cameraController, EmailService emailService, MessageSource messageSource, MusicController musicController, WifiController wifiController) {
        this.sunTimeManager = sunTimeManager;
        this.doorController = doorController;
        this.cameraController = cameraController;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.musicController = musicController;
        this.wifiController = wifiController;
    }

    public void manageDoorClosingEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (!doorController.doorIsClosed()) {
                try {
                    wifiController.turnOn();
                    logger.info("start door closing job at sunset.");
                    logger.info("take picture before closing door.");
                    cameraController.takePictureNoException(true);
                    logger.info("close door");
                    doorController.closeDoorWithBottormButtonManagement(false);
                    logger.info("take picture once the door is closed and send it by email.");
                    notification(
                            messageSource.getMessage(
                                    "event.mail.with_picture.sunset.title", null, Locale.getDefault()),
                            messageSource.getMessage("event.mail.with_picture.message", null, Locale.getDefault()),
                            messageSource.getMessage("event.mail.without_picture.message", null,
                                    Locale.getDefault()));
                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                    notification(
                            messageSource.getMessage(
                                    "event.problem.mail.with_picture.title", null, Locale.getDefault()),
                            messageSource.getMessage("event.problem.mail.with_picture.message",
                                    null, Locale.getDefault()),
                            messageSource.getMessage("event.problem.mail.without_picture.message",
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

    private void notification(String title, String textWithPicture, String textIfNoPicture) {
        Optional<File> picWithClosedDoor = cameraController.takePictureNoException(true);
        if (picWithClosedDoor.isPresent()) {
            emailService.sendMailWithAttachment(
                    title,
                    textWithPicture, picWithClosedDoor.get());
        } else {
            emailService.sendMail(
                    title,
                    textIfNoPicture);
        }
    }

    protected void setPlaySongAtSunset(boolean playSongAtSunset) {
        this.playSongAtSunset = playSongAtSunset;
    }
}
