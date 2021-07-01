package org.jibe77.hermanas.controller.startup_on_error_notif;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Component
public class ApplicationStatusListener {

    Logger logger = LoggerFactory.getLogger(ApplicationStatusListener.class);

    EventRepository eventRepository;
    CameraController cameraController;
    EmailService emailService;
    MessageSource messageSource;
    WifiController wifiController;
    ConsumptionModeController consumptionModeController;

    public ApplicationStatusListener(EventRepository eventRepository, EmailService emailService,
                                     CameraController cameraController, MessageSource messageSource,
                                     WifiController wifiController, ConsumptionModeController consumptionModeController) {
        this.eventRepository = eventRepository;
        this.emailService = emailService;
        this.cameraController = cameraController;
        this.messageSource = messageSource;
        this.wifiController = wifiController;
        this.consumptionModeController = consumptionModeController;
    }

    @PostConstruct
    public void init() {
        if (applicationHasShutdownIncorrecly()) {
            sendShutdownErrorNotification();
        }
        logger.info("Save startup time in Event Table.");
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        event.setDateTime(LocalDateTime.now());
        eventRepository.save(event);
    }

    private boolean applicationHasShutdownIncorrecly() {
        logger.info("Verify if the application has shutdown incorrectly.");
        EventType[] eventTypes = new EventType[] {EventType.STARTUP, EventType.SHUTDOWN};
        Event event = eventRepository.findTopByEventTypeInOrderByDateTimeDesc(eventTypes);
        if (event != null && event.getEventType() == EventType.STARTUP) {
            logger.info(
                    "The last event is a startup (expected the fetch a shutdown event) on {}.",
                    event.getDateTime());
            return true;
        }
        return false;
    }

    private void sendShutdownErrorNotification() {
        logger.info("Sending a shutdown error notification by email.");
        boolean initialWifiStatus = wifiController.wifiCardIsEnabled();
        if (!initialWifiStatus) {
            logger.info("application status listener is enabling the wifi card for sending an email.");
            wifiController.turnOn();
        }
        Optional<File> pic = cameraController.takePictureNoException(true);
        emailService.sendMail(
            messageSource.getMessage("restarted.incorrectly.title", null, Locale.getDefault()),
            messageSource.getMessage("restarted.incorrectly.message", null, Locale.getDefault()) +
                    messageSource.getMessage(pic.isPresent() ?
                            "restarted.incorrectly.message_with_picture" :
                            "restarted.incorrectly.message_without_picture", null, Locale.getDefault()),
                pic);
        if (!initialWifiStatus) {
            logger.info("application status listener is disabling the wifi card for sending an email.");
            wifiController.turnOff();
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Save shutdown time in Event Table.");
        Event event = new Event();
        event.setEventType(EventType.SHUTDOWN);
        event.setDateTime(LocalDateTime.now());
        eventRepository.save(event);
    }
}
