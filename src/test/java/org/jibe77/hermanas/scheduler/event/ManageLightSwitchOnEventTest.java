package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.fan.FanService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.service.music.MusicService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class ManageLightSwitchOnEventTest {

    ManageLightSwitchingOnEvent manageLightSwitchingOnEvent;

    SunTimeManager sunTimeManager;
    LightService lightService;
    DoorService doorService;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    FanService fanService;
    ConsumptionModeController consumptionMode;
    MusicService musicService;
    ConfigService configService;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);

        sunTimeManager = mock(SunTimeManager.class);
        lightService = mock(LightService.class);
        doorService = mock(DoorService.class);
        fanService = mock(FanService.class);
        consumptionMode = mock(ConsumptionModeController.class);
        musicService = mock(MusicService.class);
        configService = mock(ConfigService.class);

        manageLightSwitchingOnEvent = new ManageLightSwitchingOnEvent(
                sunTimeManager, lightService, doorService, fanService, consumptionMode, musicService,
                configService);
    }

    @Test
    void testManageLightSwitchingOnEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInThePast);
        when(configService.isSongAtSunsetEnabled()).thenReturn(true);
        manageLightSwitchingOnEvent.manageLightSwitchingOnEvent(LocalDateTime.now());
        verify(lightService, times(1)).switchOn();
        verify(musicService, times(1)).playMusicRandomly();
    }
}
