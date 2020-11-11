package org.jibe77.hermanas.scheduler.job;

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

class SunRelatedJobTest {

    SunRelatedJob sunRelatedJob;

    SunTimeManager sunTimeManager;
    CameraController cameraController;
    LightController lightController;
    EmailService emailService;
    DoorController doorController;
    MusicController musicController;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    LocalDateTime eventToLaunch;
    MessageSource messageSource;
    FanController fanController;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);
        eventToLaunch = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraController = mock(CameraController.class);
        lightController = mock(LightController.class);
        emailService = mock(EmailService.class);
        doorController = mock(DoorController.class);
        musicController = mock(MusicController.class);
        messageSource = mock(MessageSource.class);
        fanController = mock(FanController.class);

        sunRelatedJob = new SunRelatedJob(sunTimeManager, cameraController, lightController,
                emailService, doorController, musicController, messageSource, fanController);
    }

    @Test
    void testNoEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        sunRelatedJob.execute();
        verify(cameraController, times(0)).takePictureNoException(false);
        verify(lightController, times(0)).switchOff();
        verify(lightController, times(0)).switchOn();
        verify(doorController, times(0)).closeDoor(false);
        verify(doorController, times(0)).openDoor(false, false);
        verify(doorController, times(0)).openDoor(false, false);
        verify(musicController, times(0)).cocorico();
        verify(musicController, times(0)).playMusicRandomly();
        verify(musicController, times(0)).stop();
    }

    @Test
    void testManageLightSwitchingOnEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInThePast);
        sunRelatedJob.execute();
        verify(lightController, times(1)).switchOn();
    }

    @Test
    void testManageDoorOpeningEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        sunRelatedJob.execute();
        verify(doorController, times(1)).openDoor(false, false);
    }

    @Test
    void testManageDoorClosingEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        sunRelatedJob.execute();
        verify(doorController, times(1)).closeDoor(false);
    }
}