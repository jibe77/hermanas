package org.jibe77.hermanas.controller.music;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MusicController.class})
public class MusicControllerTest {

    @Autowired
    MusicController musicController;

    @Test
    void testStopWithoutCurrentProcess() {
        musicController.setCurrentMusicProcess(null);
        musicController.stop();
        assertNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testStopWithCurrentProcess() throws IOException {
        musicController.setCurrentMusicProcess(new ProcessBuilder("/bin/sleep", "5").start());
        musicController.stop();
        assertNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testPlayMusic() {
        boolean hasWorked = musicController.playMusicRandomly();
        assertTrue(hasWorked);
        assertNotNull(musicController.getCurrentMusicProcess());
    }

    @Test
    void testCocorico() {
        boolean hasWorked = musicController.cocorico();
        assertTrue(hasWorked);
        assertNotNull(musicController.getCurrentMusicProcess());
    }

    @AfterEach
    void tearDown() {
        musicController.stop();
    }


}
