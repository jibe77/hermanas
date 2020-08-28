package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.music.MusicController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        return musicController.playSongRandomly();
    }

    @GetMapping(value = "/music/stop")
    public void stop() {
        logger.info("Stop music");
        musicController.stop();
    }

    @GetMapping(value = "/music/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicController.cocorico();
    }
}
