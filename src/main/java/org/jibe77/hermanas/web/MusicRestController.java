package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/music")
@Tag(name = "Music", description = "Music player control endpoints for playing sounds in the coop")
public class MusicRestController {

    MusicService musicService;

    private static final Logger logger = LoggerFactory.getLogger(MusicRestController.class);

    public MusicRestController(MusicService musicService) {
        this.musicService = musicService;
    }

    @Operation(
            summary = "Switch music player on/off",
            description = "Turns the music player on or off"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Music player switched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @GetMapping(value = "/switch", produces = "application/json")
    public Status switcher(
            @Parameter(description = "True to turn music on, false to turn off", required = true)
            boolean param) {
        return musicService.switcher(param);
    }

    @Operation(
            summary = "Get music player status",
            description = "Returns the current status of the music player (on/off)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Music player status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @GetMapping(value = "/status")
    public Status getStatus() {
        logger.info("return music player status");
        return musicService.getStatus();
    }

    @Operation(
            summary = "Play rooster sound",
            description = "Plays the rooster crow sound (cocorico)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rooster sound played successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            )
    })
    @GetMapping(value = "/cocorico")
    public boolean cocorico() {
        logger.info("Cocorico !");
        return musicService.cocorico();
    }
}
