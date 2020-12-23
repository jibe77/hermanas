package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.fan.FanController;
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
public class ManageDoorOpeningEvent {

    SunTimeManager sunTimeManager;

    CameraController cameraController;

    DoorController doorController;

    MusicController musicController;

    FanController fanController;

    WifiController wifiController;

    EmailService emailService;

    MessageSource messageSource;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraController cameraController,
                                  DoorController doorController, MusicController musicController,
                                  FanController fanController, WifiController wifiController,
                                  EmailService emailService, MessageSource messageSource) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.doorController = doorController;
        this.musicController = musicController;
        this.fanController = fanController;
        this.wifiController = wifiController;
        this.emailService = emailService;
        this.messageSource = messageSource;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            if (!doorController.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                if (cocoricoAtSunriseEnabled) {
                    musicController.cocorico();
                }
                wifiController.turnOn();
                Optional<File> picBeforeOpening = cameraController.takePictureNoException(true);
                boolean isCorreclyOpened = doorController.openDoorWithUpButtonManagment(false, false);
                Optional<File> picAfterOpening = cameraController.takePictureNoException(true);
                if (isCorreclyOpened) {
                    if (picBeforeOpening.isPresent() && picAfterOpening.isPresent()) {
                        emailService.sendMailWithAttachment(
                                messageSource.getMessage("event.mail.with_picture.sunrise.title", null, Locale.getDefault()),
                                messageSource.getMessage("event.mail.with_picture.message", null, Locale.getDefault()),
                                picBeforeOpening.get(),
                                picAfterOpening.get()
                        );
                    } else {
                        emailService.sendMail(
                                messageSource.getMessage("event.mail.with_picture.sunrise.title", null, Locale.getDefault()),
                                messageSource.getMessage("event.mail.without_picture.message", null, Locale.getDefault())
                        );
                    }
                } else {
                    if (picBeforeOpening.isPresent() && picAfterOpening.isPresent()) {
                        emailService.sendMailWithAttachment(
                                messageSource.getMessage("event.problem.mail.with_picture.title", null, Locale.getDefault()),
                                messageSource.getMessage("event.mail.with_picture.message", null, Locale.getDefault()),
                                picBeforeOpening.get(),
                                picAfterOpening.get()
                        );
                    } else {
                        emailService.sendMail(
                                messageSource.getMessage("event.problem.mail.with_picture.title", null, Locale.getDefault()),
                                messageSource.getMessage("event.mail.without_picture.message", null, Locale.getDefault())
                        );
                    }
                }
            }
            fanController.switchOn();
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
