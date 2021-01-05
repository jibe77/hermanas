package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class NotificationService {

    public static final String RETURN_TO_NEXT_LINE = "\r\n";
    @Value("${email.notification.enabled}")
    private boolean enabled;

    private CameraController cameraController;

    private EmailService emailService;

    private SunTimeManager sunTimeManager;

    MessageSource messageSource;

    Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(EmailService emailService, CameraController cameraController,
                               MessageSource messageSource, SunTimeManager sunTimeManager) {
        this.cameraController = cameraController;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.sunTimeManager = sunTimeManager;
    }

    public void doorOpeningEvent(boolean isOpenedCorrectly, Optional<File> picBeforeOpening) {
        if (enabled) {
            // prepare title and message
            String title = messageSource.getMessage(
                    isOpenedCorrectly ? "event.mail.opening.ok.title" : "event.mail.opening.ko.title",
                    null, Locale.getDefault());
            StringBuilder message = new StringBuilder();
            message.append(messageSource.getMessage(
                    isOpenedCorrectly ? "event.mail.opening.ok.message" : "event.mail.opening.ko.message",
                    null, Locale.getDefault()));
            message.append(RETURN_TO_NEXT_LINE);

            // add closing time
            message.append(messageSource.getMessage(
                    "event.mail.opening.closing.time",
                    new Object[]{
                            sunTimeManager.getNextDoorClosingTime().format(
                                    DateTimeFormatter.ofPattern(SunTimeManager.HH_MM))}
                     ,
                    Locale.getDefault()));

            // add pictures
            Optional<File> picWithClosedDoor = cameraController.takePictureNoException(true);
            String messageKey;
            if (picBeforeOpening.isPresent() && picWithClosedDoor.isPresent()) {
                messageKey = "event.mail.with_pictures.message";
            } else if (picBeforeOpening.isPresent() || picWithClosedDoor.isPresent()) {
                messageKey = "event.mail.with_picture.message";
            } else {
                messageKey = "event.mail.without_picture.message";
            }
            message.append(messageSource.getMessage(
                    messageKey,
                    null, Locale.getDefault()));

            emailService.sendMail(
                    title,
                    message.toString(),
                    picBeforeOpening,
                    picWithClosedDoor);
        } else {
            logger.info("Notification 'doorOpeningEvent' not activated.");
        }
    }

    public void doorClosingEvent(boolean isClosedCorrectly) {
        if (enabled) {
            // prepare title and message
            String title = messageSource.getMessage(
                    isClosedCorrectly ? "event.mail.closing.ok.title" : "event.mail.closing.ko.title",
                    null, Locale.getDefault());
            StringBuilder message = new StringBuilder();
            message.append(messageSource.getMessage(
                    isClosedCorrectly ? "event.mail.closing.ok.message" : "event.mail.closing.ko.message",
                    null, Locale.getDefault()));
            message.append(RETURN_TO_NEXT_LINE);

            // add opening time
            message.append(messageSource.getMessage(
                    "event.mail.closing.opening.time",
                    new Object[]{
                            sunTimeManager.getNextDoorOpeningTime().format(
                                    DateTimeFormatter.ofPattern(SunTimeManager.HH_MM))}
                    ,
                    Locale.getDefault()));

            // add pictures
            Optional<File> picWithClosedDoor = cameraController.takePictureNoException(true);
            message.append(messageSource.getMessage(
                    picWithClosedDoor.isPresent() ?
                            "event.mail.with_picture.message" :
                            "event.mail.without_picture.message",
                    null, Locale.getDefault()));

            emailService.sendMail(
                    title,
                    message.toString(),
                    picWithClosedDoor);
        } else {
            logger.info("Notification 'doorClosingEvent' not activated.");
        }
    }
}
