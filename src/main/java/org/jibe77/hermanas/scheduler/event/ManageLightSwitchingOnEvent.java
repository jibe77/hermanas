package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.fan.FanService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ManageLightSwitchingOnEvent {

    SunTimeManager sunTimeManager;

    LightService lightService;

    DoorService doorService;

    FanService fanService;

    ConsumptionModeController consumptionModeController;

    MusicService musicService;

    @Value("${play.song.at.sunset}")
    private boolean playSongAtSunset;

    private static final Logger logger = LoggerFactory.getLogger(ManageLightSwitchingOnEvent.class);

    public ManageLightSwitchingOnEvent(SunTimeManager sunTimeManager, LightService lightService,
                                       DoorService doorService, FanService fanService,
                                       ConsumptionModeController consumptionModeController,
                                       MusicService musicService) {
        this.sunTimeManager = sunTimeManager;
        this.lightService = lightService;
        this.doorService = doorService;
        this.fanService = fanService;
        this.consumptionModeController = consumptionModeController;
        this.musicService = musicService;
    }

    public void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            if (consumptionModeController.isEcoMode(LocalDateTime.now())) {
                logger.info("light switching on event is disabled with eco mode.");
            } else {
                logger.info("light switching on event is starting now.");
                lightService.switchOn();
            }
            if (!doorService.doorIsOpened()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                doorService.openDoorWithUpButtonManagment(false, false);
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                fanService.switchOn();
            }
            if (playSongAtSunset) {
                musicService.playMusicRandomly();
            }
            sunTimeManager.reloadLightOnTime();
        }
    }

    protected void setPlaySongAtSunset(boolean playSongAtSunset) {
        this.playSongAtSunset = playSongAtSunset;
    }

}
