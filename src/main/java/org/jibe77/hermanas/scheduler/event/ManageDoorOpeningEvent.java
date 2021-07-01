package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.music.MusicController;
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

    CameraController cameraController;

    DoorController doorController;

    MusicController musicController;

    FanController fanController;

    WifiController wifiController;

    NotificationService notificationService;

    ConsumptionModeController consumptionModeController;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraController cameraController,
                                  DoorController doorController, MusicController musicController,
                                  FanController fanController, WifiController wifiController,
                                  NotificationService notificationService,
                                  ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.doorController = doorController;
        this.musicController = musicController;
        this.fanController = fanController;
        this.wifiController = wifiController;
        this.notificationService = notificationService;
        this.consumptionModeController = consumptionModeController;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            if (!doorController.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                if (cocoricoAtSunriseEnabled && !consumptionModeController.isEcoMode(LocalDateTime.now())) {
                    musicController.cocorico();
                }
                wifiController.turnOn();
                Optional<File> picBeforeOpening = cameraController.takePictureNoException(true);
                boolean isCorrectlyOpened = doorController.openDoorWithUpButtonManagment(false, false);

                notificationService.doorOpeningEvent(
                        isCorrectlyOpened,
                        picBeforeOpening
                );
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                fanController.switchOn();
            } else {
                // turn off the wifi in 15 minutes
                wifiController.turnOffAfter(900);
            }
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
