package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ManageDoorOpeningEvent {

    SunTimeManager sunTimeManager;

    CameraController cameraController;

    DoorController doorController;

    MusicController musicController;

    FanController fanController;

    WifiController wifiController;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraController cameraController, DoorController doorController, MusicController musicController, FanController fanController, WifiController wifiController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraController = cameraController;
        this.doorController = doorController;
        this.musicController = musicController;
        this.fanController = fanController;
        this.wifiController = wifiController;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorOpeningTime())) {
            if (!doorController.doorIsOpened()) {
                logger.info("door opening event is starting now.");
                wifiController.turnOn();
                cameraController.takePictureNoException(true);
                doorController.openDoorWithUpButtonManagment(false, false);
                cameraController.takePictureNoException(true);
            }
            if (cocoricoAtSunriseEnabled) {
                musicController.cocorico();
            }
            fanController.switchOn();
            sunTimeManager.reloadDoorOpeningTime();
        }
    }
}
