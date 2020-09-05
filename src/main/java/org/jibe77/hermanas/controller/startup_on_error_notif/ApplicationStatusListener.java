package org.jibe77.hermanas.controller.startup_on_error_notif;

import org.jibe77.hermanas.client.jms.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ApplicationStatusListener {

    Logger logger = LoggerFactory.getLogger(ApplicationStatusListener.class);

    EventRepository eventRepository;
    CameraController cameraController;
    EmailService emailService;

    public ApplicationStatusListener(EventRepository eventRepository, EmailService emailService, CameraController cameraController) {
        this.eventRepository = eventRepository;
        this.emailService = emailService;
        this.cameraController = cameraController;
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
        Optional<File> pic = cameraController.takePictureNoException();
        if (pic.isPresent()) {
            emailService.sendMailWithAttachment(
                    "Hermanas has restarted incorrecly",
                    "The application was not available due to a technical problem. " +
                            "Please verify the chicken coop door on the following picture !",
                    pic.get());
        } else {
            emailService.sendMail(
                    "Hermanas has restarted incorrecly",
                    "The application was not available due to a technical problem. " +
                            "Please verify the chicken coop door ! " +
                            "The picture inside the chicken coop is not available (camera problem ?).");
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
