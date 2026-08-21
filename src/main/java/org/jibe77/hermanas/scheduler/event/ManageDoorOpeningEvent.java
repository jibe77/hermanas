package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.fan.FanService;
import org.jibe77.hermanas.service.music.MusicService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ManageDoorOpeningEvent {

    SunTimeManager sunTimeManager;

    CameraService cameraService;

    DoorService doorService;

    MusicService musicService;

    FanService fanService;

    WifiService wifiService;

    NotificationService notificationService;

    ConsumptionModeController consumptionModeController;

    ConfigService configService;

    EventService eventService;

    private static final Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    /**
     * How many times a failed sunrise opening is retried on subsequent scheduler ticks
     * before giving up and rolling the schedule over to J+1. The scheduler runs every
     * 60 s, so this spreads the attempts over a few minutes — long enough to ride out a
     * bouncing end-of-course switch, short enough to avoid retrying all morning.
     */
    static final int MAX_OPENING_ATTEMPTS = 3;

    /** Failed attempts for the current sunrise window; reset once the window rolls over. */
    private int failedOpeningAttempts = 0;

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraService cameraService,
                                  DoorService doorService, MusicService musicService,
                                  FanService fanService, WifiService wifiService,
                                  NotificationService notificationService,
                                  ConsumptionModeController consumptionModeController,
                                  ConfigService configService,
                                  EventService eventService) {
        this.sunTimeManager = sunTimeManager;
        this.cameraService = cameraService;
        this.doorService = doorService;
        this.musicService = musicService;
        this.fanService = fanService;
        this.wifiService = wifiService;
        this.notificationService = notificationService;
        this.consumptionModeController = consumptionModeController;
        this.configService = configService;
        this.eventService = eventService;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            // Tracks whether the opening attempt below succeeded. Stays true when no
            // attempt was needed (door already up), so the normal path still rolls the
            // schedule over to J+1.
            boolean openingSucceeded = true;
            // Open on !doorIsOpened() rather than the stricter doorIsClosed(): a
            // hen leaning on the trap or a flaky bottom switch can release the bottom
            // sensor overnight, leaving both buttons unpressed at sunrise. The old
            // strict check silently skipped the morning opening in that case.
            // On success reloadDoorOpeningTime() below pushes the next opening to J+1;
            // on failure the window is deliberately left open for up to
            // MAX_OPENING_ATTEMPTS retries.
            if (!doorService.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                // Crow only on the first attempt of the window — a retry every 60 s
                // must not turn a flaky switch into a cocorico loop.
                if (failedOpeningAttempts == 0
                        && configService.isCocoricoAtSunriseEnabled()
                        && !consumptionModeController.isEcoMode(LocalDateTime.now())) {
                    logger.info("Triggering cocorico automatically at sunrise.");
                    if (musicService.cocorico()) {
                        eventService.recordAuto(EventType.COCORICO, "auto: sunrise");
                    }
                }
                wifiService.turnOn();
                Optional<File> picBeforeOpening = cameraService.takePictureNoException(false);
                if (picBeforeOpening.isPresent()) {
                    eventService.recordAuto(EventType.PICTURE_TAKEN, "auto: door-opening snapshot");
                }
                boolean isCorrectlyOpened = doorService.openDoorWithUpButtonManagment(false, false);
                openingSucceeded = isCorrectlyOpened;

                if (isCorrectlyOpened) {
                    eventService.recordAuto(EventType.DOOR_OPENED, "auto: sunrise");
                } else {
                    eventService.recordAuto(EventType.DOOR_OPEN_FAILED, "auto: sunrise");
                }

                notificationService.doorOpeningEvent(
                        isCorrectlyOpened,
                        picBeforeOpening
                );
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                // FanService.switchOn(details) now writes the journal entry itself;
                // passing the details string here keeps the "auto: sunrise" attribution.
                fanService.switchOn("auto: sunrise");
            } else {
                // turn off the wifi in 15 minutes
                wifiService.turnOffAfter(900);
            }
            // Only roll the schedule forward to J+1 on success. On failure we leave the
            // opening time in the past so the next scheduler tick retries, instead of
            // leaving the hens shut in until tomorrow — but give up after
            // MAX_OPENING_ATTEMPTS so a genuinely broken door doesn't retry all morning.
            if (openingSucceeded) {
                failedOpeningAttempts = 0;
                sunTimeManager.reloadDoorOpeningTime();
            } else if (++failedOpeningAttempts >= MAX_OPENING_ATTEMPTS) {
                logger.error("door opening still failing after {} attempts, giving up until tomorrow.",
                        failedOpeningAttempts);
                failedOpeningAttempts = 0;
                sunTimeManager.reloadDoorOpeningTime();
            } else {
                logger.warn("door opening failed (attempt {}/{}), retrying on the next scheduler tick.",
                        failedOpeningAttempts, MAX_OPENING_ATTEMPTS);
            }
        }
    }
}
