package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class ManageLightSwitchOnEventTest {

    ManageLightSwitchingOnEvent manageLightSwitchingOnEvent;

    SunTimeManager sunTimeManager;
    LightController lightController;
    DoorController doorController;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    FanController fanController;
    ConsumptionModeController consumptionMode;
    MusicController musicController;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);

        sunTimeManager = mock(SunTimeManager.class);
        lightController = mock(LightController.class);
        doorController = mock(DoorController.class);
        fanController = mock(FanController.class);
        consumptionMode = mock(ConsumptionModeController.class);
        musicController = mock(MusicController.class);

        manageLightSwitchingOnEvent = new ManageLightSwitchingOnEvent(
                sunTimeManager, lightController, doorController, fanController, consumptionMode, musicController);
    }

    @Test
    void testManageLightSwitchingOnEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInThePast);
        manageLightSwitchingOnEvent.setPlaySongAtSunset(true);
        manageLightSwitchingOnEvent.manageLightSwitchingOnEvent(LocalDateTime.now());
        verify(lightController, times(1)).switchOn();
        verify(musicController, times(1)).playMusicRandomly();
    }
}
