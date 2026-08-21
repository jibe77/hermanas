package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.service.event.EventService;
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
    org.jibe77.hermanas.service.config.ConfigService configService;

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
        configService = mock(org.jibe77.hermanas.service.config.ConfigService.class);
        EventService eventService = mock(EventService.class);

        manageDoorOpeningEvent = new ManageDoorOpeningEvent(
                sunTimeManager, cameraService, doorService, musicService, fanService, wifiService,
                notificationService, consumptionModeController, configService, eventService);
    }

    @Test
    void testManageDoorOpeningEvent() {
        when(sunTimeManager.getNextDoorClosingTime()).thenReturn(eventAlwaysInTheFutur);
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(sunTimeManager.getNextLightOnTime()).thenReturn(eventAlwaysInTheFutur);
        // Scheduler opens as long as the door is not already up — covers the
        // "bottom switch released overnight" case where doorIsClosed() would
        // return false and used to silently skip the morning opening.
        when(doorService.doorIsOpened()).thenReturn(false);
        when(doorService.openDoorWithUpButtonManagment(false, false)).thenReturn(true);
        manageDoorOpeningEvent.manageDoorOpeningEvent(LocalDateTime.now());
        verify(doorService, times(1)).openDoorWithUpButtonManagment(false, false);
        // Ouverture réussie : la fenêtre bascule à J+1.
        verify(sunTimeManager, times(1)).reloadDoorOpeningTime();
    }

    /**
     * Une ouverture ratée ne doit pas repousser l'échéance à J+1 : sinon les poules
     * restent enfermées toute la journée (incident du 2026-08-21). La fenêtre reste
     * ouverte pour un nouvel essai au tick suivant.
     */
    @Test
    void failedOpeningIsRetriedOnTheNextTick() {
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(doorService.doorIsOpened()).thenReturn(false);
        when(doorService.openDoorWithUpButtonManagment(false, false)).thenReturn(false);

        manageDoorOpeningEvent.manageDoorOpeningEvent(LocalDateTime.now());

        verify(doorService, times(1)).openDoorWithUpButtonManagment(false, false);
        verify(sunTimeManager, never()).reloadDoorOpeningTime();
    }

    /**
     * ... mais on n'insiste pas indéfiniment : après MAX_OPENING_ATTEMPTS échecs, on
     * abandonne jusqu'au lendemain plutôt que de réessayer toutes les minutes.
     */
    @Test
    void openingIsGivenUpAfterMaxAttempts() {
        when(sunTimeManager.getNextDoorOpeningTime()).thenReturn(eventAlwaysInThePast);
        when(doorService.doorIsOpened()).thenReturn(false);
        when(doorService.openDoorWithUpButtonManagment(false, false)).thenReturn(false);
        when(configService.isCocoricoAtSunriseEnabled()).thenReturn(true);
        when(consumptionModeController.isEcoMode(any(LocalDateTime.class))).thenReturn(false);

        for (int i = 0; i < ManageDoorOpeningEvent.MAX_OPENING_ATTEMPTS; i++) {
            manageDoorOpeningEvent.manageDoorOpeningEvent(LocalDateTime.now());
        }

        verify(doorService, times(ManageDoorOpeningEvent.MAX_OPENING_ATTEMPTS))
                .openDoorWithUpButtonManagment(false, false);
        // La fenêtre n'est repoussée qu'une seule fois, au dernier échec.
        verify(sunTimeManager, times(1)).reloadDoorOpeningTime();
        // Le cocorico ne retentit qu'au premier essai, pas à chaque retry.
        verify(musicService, times(1)).cocorico();
    }
}
