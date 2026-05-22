package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraService;
import org.jibe77.hermanas.controller.door.DoorService;
import org.jibe77.hermanas.controller.energy.WifiService;
import org.jibe77.hermanas.controller.fan.FanService;
import org.jibe77.hermanas.controller.music.MusicService;
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

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    private static final Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraService cameraService,
                                  DoorService doorService, MusicService musicService,
                                  FanService fanService, WifiService wifiService,
                                  NotificationService notificationService,
                                  ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraService = cameraService;
        this.doorService = doorService;
        this.musicService = musicService;
        this.fanService = fanService;
        this.wifiService = wifiService;
        this.notificationService = notificationService;
        this.consumptionModeController = consumptionModeController;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            if (!doorService.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                if (cocoricoAtSunriseEnabled && !consumptionModeController.isEcoMode(LocalDateTime.now())) {
                    musicService.cocorico();
                }
                wifiService.turnOn();
                Optional<File> picBeforeOpening = cameraService.takePictureNoException(true);
                boolean isCorrectlyOpened = doorService.openDoorWithUpButtonManagment(false, false);

                notificationService.doorOpeningEvent(
                        isCorrectlyOpened,
                        picBeforeOpening
                );
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                fanService.switchOn();
            } else {
                // turn off the wifi in 15 minutes
                wifiService.turnOffAfter(900);
            }
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
