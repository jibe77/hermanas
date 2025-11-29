package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicRestController {

    MusicService musicService;

    private static final Logger logger = LoggerFactory.getLogger(MusicRestController.class);

    public MusicRestController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping(value = "/music/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return musicService.switcher(param);
    }

    @GetMapping(value = "/music/status")
    public Status getStatus() {
        logger.info("return music player status");
        return musicService.getStatus();
    }

    @GetMapping(value = "/music/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicService.cocorico();
    }
}
