package org.jibe77.hermanas.service.startup_on_error_notif;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Component
public class ApplicationStatusListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStatusListener.class);

    /**
     * Classes that Spring/Tomcat/MariaDB/Logback lazy-load only during shutdown. On Spring Boot 2.7
     * with the LaunchedURLClassLoader, the JAR's URL connections can be closed before these are
     * resolved, triggering NoClassDefFoundError and breaking graceful shutdown (which then leads
     * systemd to SIGKILL the process). Touching them at startup forces the classloader to resolve
     * and cache them while everything is still healthy.
     */
    private static final String[] SHUTDOWN_CRITICAL_CLASSES = new String[] {
            "org.springframework.boot.web.server.GracefulShutdownResult",
            "org.apache.catalina.Lifecycle$SingleUse",
            "org.mariadb.jdbc.message.client.QuitPacket",
            "ch.qos.logback.core.util.ContextUtil"
    };

    EventRepository eventRepository;
    CameraService cameraService;
    EmailService emailService;
    MessageSource messageSource;
    WifiService wifiService;
    ConsumptionModeController consumptionModeController;

    public ApplicationStatusListener(EventRepository eventRepository, EmailService emailService,
                                     CameraService cameraService, MessageSource messageSource,
                                     WifiService wifiService, ConsumptionModeController consumptionModeController) {
        this.eventRepository = eventRepository;
        this.emailService = emailService;
        this.cameraService = cameraService;
        this.messageSource = messageSource;
        this.wifiService = wifiService;
        this.consumptionModeController = consumptionModeController;
    }

    @PostConstruct
    public void init() {
        preloadShutdownCriticalClasses();
        Event lastEvent = findLastStartupOrShutdown();
        if (lastEvent != null && lastEvent.getEventType() == EventType.STARTUP) {
            sendShutdownErrorNotification(lastEvent.getDateTime());
        }
        logger.info("Save startup time in Event Table.");
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        event.setDateTime(LocalDateTime.now());
        eventRepository.save(event);
    }

    private void preloadShutdownCriticalClasses() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String className : SHUTDOWN_CRITICAL_CLASSES) {
            try {
                Class.forName(className, true, cl);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                logger.warn("Could not preload shutdown-critical class {} : {}", className, e.getMessage());
            }
        }
    }

    private Event findLastStartupOrShutdown() {
        logger.info("Verify if the application has shutdown incorrectly.");
        EventType[] eventTypes = new EventType[] {EventType.STARTUP, EventType.SHUTDOWN};
        Event event = eventRepository.findTopByEventTypeInOrderByDateTimeDesc(eventTypes);
        if (event != null && event.getEventType() == EventType.STARTUP) {
            logger.info(
                    "The last event is a startup (expected the fetch a shutdown event) on {}.",
                    event.getDateTime());
        }
        return event;
    }

    private void sendShutdownErrorNotification(LocalDateTime lastStartupAt) {
        logger.info("Sending a shutdown error notification by email.");
        boolean initialWifiStatus = wifiService.wifiCardIsEnabled();
        if (!initialWifiStatus) {
            logger.info("application status listener is enabling the wifi card for sending an email.");
            wifiService.turnOn();
        }
        Optional<File> pic = cameraService.takePictureNoException(true);
        Locale locale = Locale.getDefault();
        Object[] args = restartArgs(lastStartupAt);
        // Body = generic intro + path-specific suffix. Both share the same
        // placeholder tuple so adding context anywhere stays consistent.
        String body = messageSource.getMessage("restarted.incorrectly.message", args, locale)
                + messageSource.getMessage(pic.isPresent()
                        ? "restarted.incorrectly.message_with_picture"
                        : "restarted.incorrectly.message_without_picture", args, locale);
        emailService.sendMail(
                messageSource.getMessage("restarted.incorrectly.title", args, locale),
                body,
                pic);
        if (!initialWifiStatus) {
            logger.info("application status listener is disabling the wifi card for sending an email.");
            wifiService.turnOff();
        }
    }

    /**
     * Returns the placeholder tuple consumed by the i18n bundle:
     * {@code {0}=host, {1}=detected-at, {2}=last-startup, {3}=outage-duration}.
     * Falls back to {@code "?"} for fields we cannot resolve so the email is
     * still readable on a partial environment.
     */
    private Object[] restartArgs(LocalDateTime lastStartupAt) {
        LocalDateTime now = LocalDateTime.now();
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "?";
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String detectedAt = now.format(f);
        String lastStartup = lastStartupAt != null ? lastStartupAt.format(f) : "?";
        String duration;
        if (lastStartupAt != null) {
            long minutes = java.time.Duration.between(lastStartupAt, now).toMinutes();
            duration = minutes < 60 ? minutes + " min" : (minutes / 60) + " h " + (minutes % 60) + " min";
        } else {
            duration = "?";
        }
        return new Object[]{host, detectedAt, lastStartup, duration};
    }

    /**
     * Persist the SHUTDOWN event as early as possible in the shutdown sequence — before Spring
     * starts destroying lifecycle beans (Tomcat, Hikari, EntityManagerFactory). A @PreDestroy on
     * this bean would fire too late: by then, the embedded server and connection pool are already
     * being torn down, and lazy-loaded driver classes may fail to resolve, leaving the save
     * uncommitted before the JVM is killed.
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            logger.info("Save shutdown time in Event Table (ContextClosedEvent).");
            Event shutdownEvent = new Event();
            shutdownEvent.setEventType(EventType.SHUTDOWN);
            shutdownEvent.setDateTime(LocalDateTime.now());
            eventRepository.save(shutdownEvent);
        } catch (Exception e) {
            logger.error("Failed to persist SHUTDOWN event during context close.", e);
        }
    }
}
