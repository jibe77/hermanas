package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.job.SunRelatedJob;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

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
    ConsumptionModeManager consumptionMode;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);

        sunTimeManager = mock(SunTimeManager.class);
        lightController = mock(LightController.class);
        doorController = mock(DoorController.class);
        fanController = mock(FanController.class);
        consumptionMode = mock(ConsumptionModeManager.class);

        manageLightSwitchingOnEvent = new ManageLightSwitchingOnEvent(sunTimeManager, lightController, doorController, fanController, consumptionMode);
    }

    @Test
    void testManageLightSwitchingOnEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInThePast);
        manageLightSwitchingOnEvent.manageLightSwitchingOnEvent(LocalDateTime.now());
        verify(lightController, times(1)).switchOn();
    }
}