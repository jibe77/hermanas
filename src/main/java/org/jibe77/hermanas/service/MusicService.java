package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.music.MusicController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicService {

    MusicController musicController;

    Logger logger = LoggerFactory.getLogger(MusicService.class);

    public MusicService(MusicController musicController) {
        this.musicController = musicController;
    }

    @GetMapping(value = "/music/play")
    public boolean play() {
        logger.info("Play music");
        return musicController.playMusicRandomly();
    }

    @GetMapping(value = "/music/stop")
    public void stop() {
        logger.info("Stop music");
        musicController.stop();
    }

    @GetMapping(value = "/music/status")
    public boolean isPlaying() {
        logger.info("return music player status");
        return musicController.isPlaying();
    }


    @GetMapping(value = "/music/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicController.cocorico();
    }
}
