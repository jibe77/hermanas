package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraService;
import org.jibe77.hermanas.controller.door.DoorService;
import org.jibe77.hermanas.controller.energy.WifiService;
import org.jibe77.hermanas.controller.music.MusicService;
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
    CameraService cameraService;
    EmailService emailService;
    DoorService doorService;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    LocalDateTime eventToLaunch;
    MessageSource messageSource;
    WifiService wifiService;
    NotificationService notificationService;
    ConsumptionModeController consumptionModeController;

    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);
        eventToLaunch = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraService = mock(CameraService.class);
        emailService = mock(EmailService.class);
        doorService = mock(DoorService.class);
        messageSource = mock(MessageSource.class);
        wifiService = mock(WifiService.class);
        notificationService = mock(NotificationService.class);
        consumptionModeController = mock(ConsumptionModeController.class);

        manageDoorClosingEvent = new ManageDoorClosingEvent(
                sunTimeManager, doorService, notificationService,
                messageSource, wifiService, consumptionModeController);
    }

    @Test
    void testManageDoorClosingEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        manageDoorClosingEvent.manageDoorClosingEvent(LocalDateTime.now());
        verify(doorService, times(1)).closeDoorWithBottormButtonManagement(false);
    }
}
