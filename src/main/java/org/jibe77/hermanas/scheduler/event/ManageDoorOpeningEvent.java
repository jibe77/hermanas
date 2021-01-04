package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
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
import java.util.concurrent.TimeUnit;

@Component
public class ManageDoorOpeningEvent {

    SunTimeManager sunTimeManager;

    CameraController cameraController;

    DoorController doorController;

    MusicController musicController;

    FanController fanController;

    WifiController wifiController;

    NotificationService notificationService;

    ConsumptionModeManager consumptionModeManager;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraController cameraController,
                                  DoorController doorController, MusicController musicController,
                                  FanController fanController, WifiController wifiController,
                                  NotificationService notificationService,
                                  ConsumptionModeManager consumptionModeManager) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.doorController = doorController;
        this.musicController = musicController;
        this.fanController = fanController;
        this.wifiController = wifiController;
        this.notificationService = notificationService;
        this.consumptionModeManager = consumptionModeManager;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            if (!doorController.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                if (cocoricoAtSunriseEnabled && !consumptionModeManager.isEcoMode()) {
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
            if (!consumptionModeManager.isEcoMode()) {
                fanController.switchOn();
            } else {
                // turn off the wifi in 15 minutes
                wifiController.turnOffAfter(900);
            }
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
