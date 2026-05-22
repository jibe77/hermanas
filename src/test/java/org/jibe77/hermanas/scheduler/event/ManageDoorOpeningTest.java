package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.service.fan.FanService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.service.music.MusicService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class ManageDoorOpeningTest {

    ManageDoorOpeningEvent manageDoorOpeningEvent;

    SunTimeManager sunTimeManager;
    CameraService cameraService;
    LightService lightService;
    EmailService emailService;
    DoorService doorService;
    MusicService musicService;
    LocalDateTime eventAlwaysInTheFutur;
    LocalDateTime eventAlwaysInThePast;
    MessageSource messageSource;
    FanService fanService;
    WifiService wifiService;
    NotificationService notificationService;
    ConsumptionModeController consumptionModeController;

    @BeforeEach
    void init() {
        eventAlwaysInTheFutur = LocalDateTime.now().plusHours(1);
        eventAlwaysInThePast = LocalDateTime.now().minusHours(1);
        sunTimeManager = mock(SunTimeManager.class);
        cameraService = mock(CameraService.class);
        lightService = mock(LightService.class);
        emailService = mock(EmailService.class);
        doorService = mock(DoorService.class);
        musicService = mock(MusicService.class);
        messageSource = mock(MessageSource.class);
        fanService = mock(FanService.class);
        wifiService = mock(WifiService.class);
        notificationService = mock(NotificationService.class);
        consumptionModeController = mock(ConsumptionModeController.class);

        manageDoorOpeningEvent = new ManageDoorOpeningEvent(
                sunTimeManager, cameraService, doorService, musicService, fanService, wifiService,
                notificationService, consumptionModeController);
    }

    @Test
    void testManageDoorOpeningEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        manageDoorOpeningEvent.manageDoorOpeningEvent(LocalDateTime.now());
        verify(doorService, times(1)).openDoorWithUpButtonManagment(false, false);
    }
}
