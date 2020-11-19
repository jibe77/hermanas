package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ManageLightSwitchingOnEvent {

    SunTimeManager sunTimeManager;

    LightController lightController;

    MusicController musicController;

    DoorController doorController;

    FanController fanController;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    Logger logger = LoggerFactory.getLogger(ManageLightSwitchingOnEvent.class);

    public ManageLightSwitchingOnEvent(SunTimeManager sunTimeManager, LightController lightController, MusicController musicController, DoorController doorController, FanController fanController) {
        this.sunTimeManager = sunTimeManager;
        this.lightController = lightController;
        this.musicController = musicController;
        this.doorController = doorController;
        this.fanController = fanController;
    }

    public void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            logger.info("light switching on event is starting now.");
            lightController.switchOn();
            if (playSongAtSunset) {
                musicController.playMusicRandomly();
            }
            if (!doorController.doorIsOpened()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorController.openDoor(false, false);
            }
            fanController.switchOn();
            sunTimeManager.reloadLightOnTime();
        }
    }
}
