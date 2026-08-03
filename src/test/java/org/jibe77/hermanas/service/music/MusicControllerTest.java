package org.jibe77.hermanas.service.music;

import org.jibe77.hermanas.service.ProcessLauncher;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.energy.SoundCardService;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.websocket.NotificationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MusicService.class})
class MusicControllerTest {

    @Autowired
    MusicService musicService;

    @MockitoBean
    ProcessLauncher processLauncher;

    @MockitoBean
    ConsumptionModeController consumptionModeController;

    @MockitoBean
    GpioHermanasService gpioHermanasService;

    @MockitoBean
    SoundCardService soundCardService;

    @MockitoBean
    NotificationController notificationController;

    @MockitoBean
    ConfigService configService;

    @MockitoBean
    EventService eventService;

    @Test
    void testStopWithoutCurrentProcess() {
        musicService.setCurrentMusicProcess(null);
        musicService.stop();
        assertNull(musicService.getCurrentMusicProcess());
    }

    @Test
    void testStopWithCurrentProcess() throws IOException {
        Process process = Mockito.mock(Process.class);
        musicService.setCurrentMusicProcess(process);
        musicService.stop();
        assertNull(musicService.getCurrentMusicProcess());
    }

    @Test
    void testPlayMusic() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenReturn(Mockito.mock(Process.class));
        Mockito.when(consumptionModeController.getDuration(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(LocalDateTime.class))).thenReturn(10000L);
        boolean hasWorked = musicService.playMusicRandomly();
        assertTrue(hasWorked);
        assertNotNull(musicService.getCurrentMusicProcess());
    }

    @Test
    void testPlayMusicWithIOException() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenThrow(new IOException());
        boolean hasWorked = musicService.playMusicRandomly();
        assertFalse(hasWorked);
        assertNull(musicService.getCurrentMusicProcess());
    }

    /**
     * Le volume configuré doit partir vers la carte son immédiatement, sans attendre
     * la lecture suivante. La carte étant coupée hors lecture, elle est allumée le
     * temps du réglage puis refermée.
     */
    @Test
    void applyConfiguredVolumeSendsItToTheSoundCard() throws IOException {
        Mockito.when(configService.getMusicVolumeRegular()).thenReturn("75%");
        musicService.setCurrentMusicProcess(null);

        assertTrue(musicService.applyConfiguredVolume());

        Mockito.verify(soundCardService).turnOn();
        Mockito.verify(processLauncher).launch(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq("75%"));
        Mockito.verify(soundCardService).turnOff();
    }

    /**
     * Un échec d'amixer ne doit pas remonter : le paramètre est déjà sauvegardé en
     * base et s'appliquera de toute façon à la lecture suivante.
     */
    @Test
    void applyConfiguredVolumeSwallowsHardwareFailure() throws IOException {
        Mockito.when(configService.getMusicVolumeRegular()).thenReturn("75%");
        Mockito.when(processLauncher.launch(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new IOException("amixer absent"));

        assertFalse(musicService.applyConfiguredVolume());
        // La carte son est refermée même en cas d'échec.
        Mockito.verify(soundCardService).turnOff();
    }

    @Test
    void testCocorico() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenReturn(Mockito.mock(Process.class));
        Mockito.when(consumptionModeController.getDuration(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(LocalDateTime.class))).thenReturn(10000L);
        boolean hasWorked = musicService.cocorico();
        assertTrue(hasWorked);
        assertNotNull(musicService.getCurrentMusicProcess());
    }

    @Test
    void testCocoricoWithException() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenThrow(new IOException());
        boolean hasWorked = musicService.cocorico();
        assertFalse(hasWorked);
        assertNull(musicService.getCurrentMusicProcess());
    }

    @AfterEach
    void tearDown() {
        musicService.stop();
    }


}
