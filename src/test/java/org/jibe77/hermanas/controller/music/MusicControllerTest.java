package org.jibe77.hermanas.controller.music;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.controller.energy.SoundCardController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.jibe77.hermanas.websocket.NotificationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MusicController.class})
class MusicControllerTest {

    @Autowired
    MusicController musicController;

    @MockBean
    ProcessLauncher processLauncher;

    @MockBean
    ConsumptionModeManager consumptionModeManager;

    @MockBean
    GpioHermanasController gpioHermanasController;

    @MockBean
    SoundCardController soundCardController;

    @MockBean
    NotificationController notificationController;

    @Test
    void testStopWithoutCurrentProcess() {
        musicController.setCurrentMusicProcess(null);
        musicController.stop();
        assertNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testStopWithCurrentProcess() throws IOException {
        Process process = Mockito.mock(Process.class);
        musicController.setCurrentMusicProcess(process);
        musicController.stop();
        assertNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testPlayMusic() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenReturn(Mockito.mock(Process.class));
        Mockito.when(consumptionModeManager.getDuration(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(10000L);
        boolean hasWorked = musicController.playMusicRandomly();
        assertTrue(hasWorked);
        assertNotNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testPlayMusicWithIOException() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenThrow(new IOException());
        boolean hasWorked = musicController.playMusicRandomly();
        assertFalse(hasWorked);
        assertNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testCocorico() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenReturn(Mockito.mock(Process.class));
        Mockito.when(consumptionModeManager.getDuration(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(10000L);
        boolean hasWorked = musicController.cocorico();
        assertTrue(hasWorked);
        assertNotNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testCocoricoWithException() throws IOException {
        Mockito.when(processLauncher.launch(Mockito.anyList())).thenThrow(new IOException());
        boolean hasWorked = musicController.cocorico();
        assertFalse(hasWorked);
        assertNull(musicController.getCurrentMusicProcess());
    }

    @AfterEach
    void tearDown() {
        musicController.stop();
    }


}
