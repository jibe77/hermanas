package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.music.MusicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/music")
@Tag(name = "Music", description = "Music player control endpoints for playing sounds in the coop")
public class MusicRestController {

    MusicService musicService;
    ConfigService configService;
    EventService eventService;
    ConsumptionModeController consumptionModeController;

    private static final Logger logger = LoggerFactory.getLogger(MusicRestController.class);

    public MusicRestController(MusicService musicService, ConfigService configService,
                               EventService eventService,
                               ConsumptionModeController consumptionModeController) {
        this.musicService = musicService;
        this.configService = configService;
        this.eventService = eventService;
        this.consumptionModeController = consumptionModeController;
    }

    @Operation(
            summary = "Switch music player on/off",
            description = "Turns the music player on or off. When turning on, optionally plays a specific playlist; " +
                    "otherwise the currently selected playlist is used."
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
            boolean param,
            @Parameter(description = "Optional playlist name (sub-directory of music.path.mix)")
            @RequestParam(value = "playlist", required = false) String playlist) {
        Status status = musicService.switcher(param, playlist);
        eventService.record(status.getStatusEnum() == StatusEnum.ON
                ? EventType.MUSIC_STARTED : EventType.MUSIC_STOPPED,
                playlist != null && !playlist.isEmpty() ? "playlist=" + playlist : null);
        return status;
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
            summary = "Get the playback duration that will apply on the next play",
            description = "Returns the duration (in milliseconds) the music will play for if started right now, " +
                    "based on the current energy mode (eco / regular / sunny). The UI can show this so the user " +
                    "knows how long the playback will last before the security timer stops it."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Duration retrieved")
    })
    @GetMapping(value = "/play-duration", produces = "application/json")
    public Map<String, Long> getPlayDuration() {
        long durationMs = consumptionModeController.getDuration(
                configService.getLightSecurityTimerDelayEco(),
                configService.getLightSecurityTimerDelayRegular(),
                configService.getLightSecurityTimerDelaySunny(),
                LocalDateTime.now());
        return Collections.singletonMap("durationMs", durationMs);
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
        boolean played = musicService.cocorico();
        if (played) {
            eventService.record(EventType.COCORICO);
        }
        return played;
    }

    @Operation(
            summary = "List available playlists",
            description = "Returns the names of every sub-directory of music.path.mix on the Pi."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlists listed successfully")
    })
    @GetMapping(value = "/playlists", produces = "application/json")
    public List<String> listPlaylists() {
        return musicService.listPlaylists();
    }

    @Operation(
            summary = "List songs of a playlist",
            description = "Returns the audio filenames (mp3, ogg, wav, flac, m4a, aac, wma, opus) inside the given playlist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Songs listed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or non-existent playlist")
    })
    @GetMapping(value = "/playlists/{name}/songs", produces = "application/json")
    public ResponseEntity<List<String>> listSongs(
            @Parameter(description = "Playlist name", required = true) @PathVariable("name") String name) {
        try {
            return ResponseEntity.ok(musicService.listSongs(name));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid playlist requested: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @Operation(
            summary = "Get the currently selected playlist",
            description = "Returns the playlist name persisted in configuration. Empty string means no selection."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Selection retrieved successfully")
    })
    @GetMapping(value = "/selected-playlist", produces = "application/json")
    public Map<String, String> getSelectedPlaylist() {
        return Collections.singletonMap("playlist", configService.getSelectedPlaylist());
    }

    @Operation(
            summary = "Set the currently selected playlist",
            description = "Persists the playlist selection in configuration. The selected playlist is used by " +
                    "scheduled events (sunrise/sunset) and by /switch when no explicit playlist is provided."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Selection updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or non-existent playlist")
    })
    @PutMapping(value = "/selected-playlist", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, String>> setSelectedPlaylist(@RequestBody Map<String, String> body) {
        String playlist = body == null ? null : body.get("playlist");
        // Validate that the playlist exists (unless the caller is clearing the selection).
        if (playlist != null && !playlist.trim().isEmpty()) {
            try {
                musicService.listSongs(playlist);
            } catch (IllegalArgumentException e) {
                logger.warn("Rejecting invalid playlist selection: {}", e.getMessage());
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
            }
        }
        configService.setSelectedPlaylist(playlist);
        return ResponseEntity.ok(Collections.singletonMap("playlist",
                playlist == null ? "" : playlist));
    }
}
