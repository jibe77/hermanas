package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class ManageDoorClosingTest {

    ManageDoorClosingEvent manageDoorClosingEvent;

    SunTimeManager sunTimeManager;
    CameraController cameraController;
    EmailService emailService;
    DoorController doorController;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    LocalDateTime eventToLaunch;
    MessageSource messageSource;
    MusicController musicController;
    WifiController wifiController;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);
        eventToLaunch = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraController = mock(CameraController.class);
        emailService = mock(EmailService.class);
        doorController = mock(DoorController.class);
        messageSource = mock(MessageSource.class);
        musicController = mock(MusicController.class);
        wifiController = mock(WifiController.class);

        manageDoorClosingEvent = new ManageDoorClosingEvent(sunTimeManager, doorController, cameraController, emailService, messageSource, musicController, wifiController);
    }

    @Test
    void testManageDoorClosingEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        manageDoorClosingEvent.setPlaySongAtSunset(true);
        manageDoorClosingEvent.manageDoorClosingEvent(LocalDateTime.now());
        verify(doorController, times(1)).closeDoorWithBottormButtonManagement(false);
        verify(musicController, times(1)).playMusicRandomly();
    }
}
