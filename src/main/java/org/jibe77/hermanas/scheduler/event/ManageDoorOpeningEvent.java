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
            // Use the strict closed check instead of the lax !doorIsOpened():
            // both buttons can be released at once (door in transit or a faulty
            // switch), and !doorIsOpened() would re-trigger the cocorico every
            // minute in that situation. doorIsClosed() reads the bottom button
            // and is the only signal that unambiguously says "still waiting to
            // open the door this morning."
            if (doorService.doorIsClosed()) {
                logger.info("door opening event is starting now.");
                if (configService.isCocoricoAtSunriseEnabled() && !consumptionModeController.isEcoMode(LocalDateTime.now())) {
                    logger.info("Triggering cocorico automatically at sunrise.");
                    if (musicService.cocorico()) {
                        eventService.recordAuto(EventType.COCORICO, "auto: sunrise");
                    }
                }
                wifiService.turnOn();
                Optional<File> picBeforeOpening = cameraService.takePictureNoException(true);
                if (picBeforeOpening.isPresent()) {
                    eventService.recordAuto(EventType.PICTURE_TAKEN, "auto: door-opening snapshot");
                }
                boolean isCorrectlyOpened = doorService.openDoorWithUpButtonManagment(false, false);

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
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
