package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.abstract_model.Status;
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

    @GetMapping(value = "/music/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return musicController.switcher(param);
    }

    @GetMapping(value = "/music/status")
    public Status getStatus() {
        logger.info("return music player status");
        return musicController.getStatus();
    }

    @GetMapping(value = "/music/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicController.cocorico();
    }
}
