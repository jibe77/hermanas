package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.fan.FanService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    ConfigService configService;

    EventService eventService;

    private static final Logger logger = LoggerFactory.getLogger(ManageLightSwitchingOnEvent.class);

    public ManageLightSwitchingOnEvent(SunTimeManager sunTimeManager, LightService lightService,
                                       DoorService doorService, FanService fanService,
                                       ConsumptionModeController consumptionModeController,
                                       MusicService musicService,
                                       ConfigService configService,
                                       EventService eventService) {
        this.sunTimeManager = sunTimeManager;
        this.lightService = lightService;
        this.doorService = doorService;
        this.fanService = fanService;
        this.consumptionModeController = consumptionModeController;
        this.musicService = musicService;
        this.configService = configService;
        this.eventService = eventService;
    }

    public void manageLightSwitchingOnEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextLightOnTime())) {
            if (consumptionModeController.isEcoMode(LocalDateTime.now())) {
                logger.info("light switching on event is disabled with eco mode.");
            } else {
                logger.info("light switching on event is starting now.");
                lightService.switchOn("auto: before sunset");
            }
            // Mirror ManageDoorOpeningEvent: trust the strict "closed" sensor
            // rather than the lax "not opened" check, so a flaky switch can't
            // make us slam the servo against an already-open door every minute.
            if (doorService.doorIsClosed()) {
                logger.info("the light-switching-on event has found that the door is closed, opening it now.");
                boolean opened = doorService.openDoorWithUpButtonManagment(false, false);
                eventService.recordAuto(
                        opened ? EventType.DOOR_OPENED : EventType.DOOR_OPEN_FAILED,
                        "auto: light-switching-on event found the door closed");
            }
            if (!consumptionModeController.isEcoMode(LocalDateTime.now())) {
                fanService.switchOn("auto: before sunset");
            }
            if (configService.isSongAtSunsetEnabled()) {
                // MusicService.playMusicRandomly writes the journal entry itself
                // (see playMusic — also covers the manual /switch path).
                musicService.playMusicRandomly();
            }
            sunTimeManager.reloadLightOnTime();
        }
    }

}
