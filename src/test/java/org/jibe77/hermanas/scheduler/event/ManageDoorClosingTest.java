package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.controller.music.MusicController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
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
    WifiController wifiController;
    NotificationService notificationService;
    ConsumptionModeController consumptionModeController;

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
        wifiController = mock(WifiController.class);
        notificationService = mock(NotificationService.class);
        consumptionModeController = mock(ConsumptionModeController.class);

        manageDoorClosingEvent = new ManageDoorClosingEvent(
                sunTimeManager, doorController, notificationService,
                messageSource, wifiController, consumptionModeController);
    }

    @Test
    void testManageDoorClosingEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        manageDoorClosingEvent.manageDoorClosingEvent(LocalDateTime.now());
        verify(doorController, times(1)).closeDoorWithBottormButtonManagement(false);
    }
}
