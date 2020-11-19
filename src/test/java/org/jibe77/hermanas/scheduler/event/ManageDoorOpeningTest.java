package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.fan.FanController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class ManageDoorOpeningTest {

    ManageDoorOpeningEvent manageDoorOpeningEvent;

    SunTimeManager sunTimeManager;
    CameraController cameraController;
    LightController lightController;
    EmailService emailService;
    DoorController doorController;
    MusicController musicController;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    MessageSource messageSource;
    FanController fanController;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraController = mock(CameraController.class);
        lightController = mock(LightController.class);
        emailService = mock(EmailService.class);
        doorController = mock(DoorController.class);
        musicController = mock(MusicController.class);
        messageSource = mock(MessageSource.class);
        fanController = mock(FanController.class);

        manageDoorOpeningEvent = new ManageDoorOpeningEvent(
                sunTimeManager, cameraController, doorController, musicController, fanController);
    }

    @Test
    void testManageDoorOpeningEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        manageDoorOpeningEvent.manageDoorOpeningEvent(LocalDateTime.now());
        verify(doorController, times(1)).openDoor(false, false);
    }
}