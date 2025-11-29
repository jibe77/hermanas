package org.jibe77.hermanas.controller.music;

import org.jibe77.hermanas.service.ProcessLauncher;
import org.jibe77.hermanas.controller.config.ConfigService;
import org.jibe77.hermanas.controller.energy.SoundCardService;
import org.jibe77.hermanas.controller.gpio.GpioHermanasService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.websocket.NotificationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MusicService.class})
class MusicControllerTest {

    @Autowired
    MusicService musicService;

    @MockBean
    ProcessLauncher processLauncher;

    @MockBean
    ConsumptionModeController consumptionModeController;

    @MockBean
    GpioHermanasService gpioHermanasService;

    @MockBean
    SoundCardService soundCardService;

    @MockBean
    NotificationController notificationController;

    @MockBean
    ConfigService configService;

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
