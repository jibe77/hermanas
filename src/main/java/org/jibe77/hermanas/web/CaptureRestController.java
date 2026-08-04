package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.jibe77.hermanas.service.capture.CaptureService;
import org.jibe77.hermanas.service.capture.CaptureStateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Lazy;

import java.util.Collections;
import java.util.Map;

/**
 * Async take-picture + AI-analyze pipeline exposed to the SPA.
 *
 * <p>The previous synchronous endpoints ({@code GET /camera/takePicture},
 * {@code GET /camera/analyze}) regularly triggered 504s at the Caddy reverse
 * proxy: the Pi Zero takes 5-30 s to grab a high-quality picture, the local
 * LLM takes 15-25 s more, and the SPA used to wait on a single HTTP request
 * spanning the full pipeline. With these three endpoints the longest call the
 * client makes is the image GET, which is served instantly from memory the
 * moment the capture phase finishes.</p>
 *
 * <p>Lifecycle: client POSTs to {@code /captures}, gets a {@code captureId},
 * then polls {@code /captures/{id}/status} every second while requesting
 * {@code /captures/{id}/image} in parallel. Image becomes available before the
 * analysis is done so the operator sees the photo while the LLM is still
 * thinking.</p>
 */
@Lazy
@RestController
@RequestMapping("/api/v1/captures")
@Tag(name = "Captures",
        description = "Async take-picture + AI-analyze pipeline. POST kicks the job, GET endpoints poll for progress.")
public class CaptureRestController {

    private static final Logger logger = LoggerFactory.getLogger(CaptureRestController.class);

    private final CaptureService captureService;

    public CaptureRestController(CaptureService captureService) {
        this.captureService = captureService;
    }

    @Operation(
            summary = "Start an async capture pipeline, optionally with AI analysis",
            description = "Returns immediately with a capture id; the actual work runs in the "
                        + "background. Pass analyze=false to only take the picture — the Webcam "
                        + "page does this on load, and asks for the analysis on demand."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Capture job accepted, polling URLs returned")
    })
    @PostMapping
    @RateLimited(maxRequests = 5, windowSeconds = 60,
            message = "Too many capture requests. Please wait a minute before trying again.")
    public ResponseEntity<Map<String, String>> start(
            @Parameter(description = "Output language for the AI analysis (fr / en / ro)", example = "fr")
            @RequestParam(defaultValue = "en") String lang,
            @Parameter(description = "Run the AI analysis after the capture", example = "true")
            @RequestParam(defaultValue = "true") boolean analyze) {
        // CaptureService.startAsync() already logs the captureId at INFO with the lang;
        // no need to duplicate here.
        String id = captureService.startAsync(lang, analyze);
        return ResponseEntity.accepted()
                .body(Collections.singletonMap("captureId", id));
    }

    @Operation(
            summary = "Get the captured image",
            description = "Returns 404 while the capture phase is still running, the JPEG bytes once available."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JPEG image bytes"),
            @ApiResponse(responseCode = "404", description = "Capture id unknown or picture not ready yet")
    })
    @GetMapping(value = "/{id}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable String id) {
        return captureService.getImage(id)
                .map(bytes -> {
                    // DEBUG: the SPA may hit this multiple times (cache busting on the img tag)
                    // and we don't want one INFO line per poll cluttering the journal.
                    logger.debug("Capture {} image served ({} bytes).", id, bytes.length);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(bytes);
                })
                .orElseGet(() -> {
                    // Polling clients hit this every 500 ms while the camera is still
                    // capturing, so we keep it at DEBUG to avoid flooding the log.
                    logger.debug("Capture {} image not ready yet (404).", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @Operation(
            summary = "Poll the capture status",
            description = "Returns the current pipeline state, plus the analysis text once status=DONE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current state"),
            @ApiResponse(responseCode = "404", description = "Capture id unknown or expired")
    })
    @GetMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaptureStateDto> status(@PathVariable String id) {
        return captureService.getStatus(id)
                .map(dto -> {
                    logger.debug("Capture {} status poll: status={} imageAvailable={}.",
                            id, dto.getStatus(), dto.isImageAvailable());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.debug("Capture {} status: not found (expired or unknown id).", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }
}
