package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    LocalDateTime eventToLaunch;


    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventToLaunch = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraController = mock(CameraController.class);
        lightController = mock(LightController.class);
        doorController = mock(DoorController.class);
        musicController = mock(MusicController.class);

        sunRelatedJob = new SunRelatedJob(sunTimeManager, cameraController, lightController,
                emailService, doorController, musicController);
    }

    @Test
    void testNoEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOffTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        sunRelatedJob.execute();
        verify(cameraController, times(0)).takePictureNoException();
        verify(lightController, times(0)).switchOff();
        verify(lightController, times(0)).switchOn();
        verify(doorController, times(0)).closeDoorWithBottormButtonManagement(false);
        verify(doorController, times(0)).openDoor(false);
        verify(doorController, times(0)).openDoor(false);
        verify(musicController, times(0)).cocorico();
        verify(musicController, times(0)).playMusicRandomly();
        verify(musicController, times(0)).stop();

    }
}