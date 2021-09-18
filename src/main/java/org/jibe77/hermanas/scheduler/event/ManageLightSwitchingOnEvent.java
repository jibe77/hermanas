package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.controller.music.MusicController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ManageLightSwitchingOnEvent {

    SunTimeManager sunTimeManager;

    LightController lightController;

    DoorController doorController;

    FanController fanController;

    ConsumptionModeController consumptionModeController;

    MusicController musicController;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    Logger logger = LoggerFactory.getLogger(ManageLightSwitchingOnEvent.class);

    public ManageLightSwitchingOnEvent(SunTimeManager sunTimeManager, LightController lightController,
                                       DoorController doorController, FanController fanController,
                                       ConsumptionModeController consumptionModeController,
                                       MusicController musicController) {
        this.sunTimeManager = sunTimeManager;
        this.lightController = lightController;
        this.doorController = doorController;
        this.fanController = fanController;
        this.consumptionModeController = consumptionModeController;
        this.musicController = musicController;
    }

    public void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            if (consumptionModeController.isEcoMode(LocalDateTime.now())) {
                logger.info("light switching on event is disabled with eco mode.");
            } else {
                logger.info("light switching on event is starting now.");
                lightController.switchOn();
            }
            if (!doorController.doorIsOpened()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorController.openDoorWithUpButtonManagment(false, false);
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                fanController.switchOn();
            }
            if (playSongAtSunset) {
                musicController.playMusicRandomly();
            }
            sunTimeManager.reloadLightOnTime();
        }
    }

    protected void setPlaySongAtSunset(boolean playSongAtSunset) {
        this.playSongAtSunset = playSongAtSunset;
    }

}
