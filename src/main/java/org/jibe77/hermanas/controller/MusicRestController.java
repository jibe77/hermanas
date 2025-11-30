package org.jibe77.hermanas.service;

import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.controller.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/music")
public class MusicRestController {

    MusicService musicService;

    private static final Logger logger = LoggerFactory.getLogger(MusicRestController.class);

    public MusicRestController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping(value = "/switch", produces = "application/json")
    public Status switcher(boolean param) {
        return musicService.switcher(param);
    }

    @GetMapping(value = "/status")
    public Status getStatus() {
        logger.info("return music player status");
        return musicService.getStatus();
    }

    @GetMapping(value = "/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicService.cocorico();
    }
}
