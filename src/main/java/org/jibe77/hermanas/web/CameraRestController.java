package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.client.ai.AiVisionCache;
import org.jibe77.hermanas.client.ai.AiVisionClient;
import org.jibe77.hermanas.client.ai.AiVisionException;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.camera.PhotosService;
import org.jibe77.hermanas.service.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/camera")
@Tag(name = "Camera", description = "Camera control endpoints for taking pictures, streaming video, and monitoring door closing")
public class CameraRestController {

    CameraService cameraService;
    private final PhotosService photosService;
    private final AiVisionClient aiVisionClient;
    private final AiVisionCache aiVisionCache;
    private final EventService eventService;

    public CameraRestController(CameraService cameraService,
                                PhotosService photosService,
                                AiVisionClient aiVisionClient,
                                AiVisionCache aiVisionCache,
                                EventService eventService) {
        this.cameraService = cameraService;
        this.photosService = photosService;
        this.aiVisionClient = aiVisionClient;
        this.aiVisionCache = aiVisionCache;
        this.eventService = eventService;
    }

    private static final Logger logger = LoggerFactory.getLogger(CameraRestController.class);

    @Operation(
            summary = "Take a picture",
            description = "Captures a photo from the camera and returns it as a PNG image"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Picture captured successfully",
                    content = @Content(mediaType = "image/jpeg")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Camera error or I/O error",
                    content = @Content
            )
    })
    @GetMapping(value = "/takePicture", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] takePicture(
            @Parameter(description = "Use high quality settings for picture capture", example = "false")
            @RequestParam(defaultValue = "false") String highQuality,
            @Parameter(description = "Force a fresh capture even if a recent one is cached", example = "false")
            @RequestParam(defaultValue = "false") String force) throws IOException, InterruptedException {
        boolean forced = Boolean.parseBoolean(force);
        File picture = cameraService.takePictureCached(
                Boolean.parseBoolean(highQuality), forced);
        logger.info("return picture from {}.", picture.getAbsolutePath());
        // Only journal a manual fresh shot — every dashboard tab polls /takePicture
        // every couple of seconds without force=true and would otherwise drown
        // the Journalisation page in PICTURE_TAKEN rows.
        if (forced) {
            eventService.record(EventType.PICTURE_TAKEN, "manual: webcam refresh");
        }
        try (FileInputStream fileInputStream = new FileInputStream(picture)) {
            return IOUtils.toByteArray(fileInputStream);
        }
    }

    @Operation(
            summary = "Start video stream",
            description = "Starts streaming video from the camera in MJPEG format"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Video stream started successfully",
                    content = @Content(mediaType = "multipart/x-mixed-replace")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Camera streaming error",
                    content = @Content
            )
    })
    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(final HttpServletResponse response) throws IOException {
        cameraService.stream();

        // mjpg_streamer needs a fraction of a second to bind port 8081 when it
        // was just started by cameraService.stream(). Retry the upstream
        // connection a few times instead of a blind Thread.sleep so a hot
        // (already-running) stream returns instantly while a cold start still
        // succeeds.
        URL streamUrl = new URL("http://localhost:8081/?action=stream");
        HttpURLConnection upstream = null;
        IOException lastError = null;
        // Budget: 80 x 100 ms = 8 s. On a hot stream this exits on the first
        // attempt. 8 s gives the Pi Zero plenty of room to spawn mjpg_streamer
        // and bind port 8081 — measured cold start was ~2 s on a loaded Pi.
        long t0 = System.currentTimeMillis();
        int attemptCount = 0;
        for (int attempt = 0; attempt < 80; attempt++) {
            attemptCount = attempt + 1;
            try {
                HttpURLConnection conn = (HttpURLConnection) streamUrl.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(10000);
                conn.setUseCaches(false);
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.connect();
                int code = conn.getResponseCode();
                if (code >= 400) {
                    conn.disconnect();
                    throw new IOException("mjpg_streamer responded HTTP " + code);
                }
                upstream = conn;
                break;
            } catch (IOException e) {
                lastError = e;
                // First failure goes to INFO so it's visible without DEBUG; we
                // expect a few "connection refused" right after starting
                // mjpg_streamer. Avoid spamming on every retry.
                if (attempt == 0) {
                    logger.info("mjpg_streamer not ready yet ({}). Retrying...", e.getMessage());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for mjpg_streamer", ie);
                }
            }
        }
        long waitMs = System.currentTimeMillis() - t0;
        if (upstream == null) {
            logger.warn("Could not reach mjpg_streamer on localhost:8081 after {} attempts ({} ms).",
                    attemptCount, waitMs, lastError);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("mjpg_streamer not reachable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (attemptCount > 1) {
            logger.info("mjpg_streamer reached after {} attempts ({} ms).", attemptCount, waitMs);
        }

        // Mirror the real Content-Type (with the boundary mjpg_streamer chose)
        // back to the browser. Hard-coding "boundarydonotcross" used to cause
        // some browsers to wait forever for a frontier that never matched.
        String upstreamContentType = upstream.getContentType();
        if (upstreamContentType == null || !upstreamContentType.startsWith("multipart/")) {
            upstreamContentType = "multipart/x-mixed-replace;boundary=boundarydonotcross";
        }
        response.setContentType(upstreamContentType);
        response.setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Connection", "close");
        // Disable proxy buffering (nginx in front of the Pi) so MJPEG frames
        // are flushed to the browser as soon as they arrive.
        response.setHeader("X-Accel-Buffering", "no");

        final HttpURLConnection finalUpstream = upstream;
        StreamingResponseBody streamingResponseBody = outputStream -> {
            // 16 KB matches a typical JPEG frame at 480x270, so we flush close
            // to once per frame instead of mid-frame.
            byte[] data = new byte[16384];
            try (InputStream in = finalUpstream.getInputStream()) {
                int size;
                while ((size = in.read(data)) != -1) {
                    outputStream.write(data, 0, size);
                    outputStream.flush();
                }
            } catch (ClientAbortException e) {
                logger.info("Client disconnected from stream, stopping mjpg_streamer.");
                try {
                    stopStream();
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while stopping stream.", ex);
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                logger.warn("Upstream MJPEG read failed.", e);
            } finally {
                finalUpstream.disconnect();
            }
        };
        return new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
    }

    @Operation(
            summary = "Stop video stream",
            description = "Stops the currently active video stream from the camera"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Video stream stopped successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error stopping video stream",
                    content = @Content
            )
    })
    @GetMapping(value = "/stopStream")
    public void stopStream() throws InterruptedException, IOException {
        cameraService.stopStream();
    }

    @Operation(
            summary = "Get door closing rate",
            description = "Returns a percentage indicating how much the door is closed based on image analysis"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Closing rate retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Integer.class))
            )
    })
    @GetMapping("/closingRate")
    public int closingRate() {
        return cameraService.getClosingRate();
    }

    @Operation(
            summary = "List entries inside the photos directory",
            description = "Returns the directories and image files at the given relative path " +
                    "(omit `path` for the root). Only .jpg / .jpeg / .png files are returned; " +
                    "directory traversal is rejected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listing returned"),
            @ApiResponse(responseCode = "400", description = "Invalid path (traversal or non-directory)")
    })
    @GetMapping("/photos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listPhotos(
            @Parameter(description = "Relative path inside the photos directory, e.g. '2026/05'")
            @RequestParam(value = "path", required = false) String path) {
        try {
            return ResponseEntity.ok(photosService.list(path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            logger.warn("Failed to list photos at '{}'", path, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Download a picture from the photos archive",
            description = "Streams the requested image file (.jpg / .jpeg / .png) inside the " +
                    "photos directory. Authentication is required to keep the archive private."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image streamed",
                    content = @Content(mediaType = "image/*")),
            @ApiResponse(responseCode = "400", description = "Invalid path"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/photos/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> downloadPhoto(
            @Parameter(description = "Relative path of the picture inside the photos directory")
            @RequestParam("path") String path) {
        try {
            Path file = photosService.resolveImageFile(path);
            String name = file.getFileName().toString().toLowerCase();
            MediaType mediaType = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Captures a high-quality snapshot and forwards it to the configured local
     * multimodal model for a chicken-coop analysis (count chickens, eggs, hay
     * level, door state, dirt level, fan dust). The model is told to answer in
     * the language passed via the {@code lang} query parameter so the rendered
     * text matches the SPA locale.
     */
    @Operation(
            summary = "Run AI analysis on the latest snapshot",
            description = "Captures a fresh high-quality picture, forwards it to the configured "
                    + "OpenAI-compatible inference server (see ai.inference.url / ai.inference.model), "
                    + "and returns the model's textual analysis in the requested language."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis completed"),
            @ApiResponse(responseCode = "501", description = "AI inference URL not configured"),
            @ApiResponse(responseCode = "502", description = "Inference server unreachable or returned an error"),
            @ApiResponse(responseCode = "500", description = "Camera or I/O error")
    })
    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimited(maxRequests = 10, windowSeconds = 60,
            message = "Too many analysis requests. Please wait a minute.")
    public ResponseEntity<java.util.Map<String, String>> analyzeSnapshot(
            @Parameter(description = "Output language (fr / en / ro). Anything else falls back to English.",
                    example = "en")
            @RequestParam(defaultValue = "en") String lang) {
        String normalized = normalizeLang(lang);
        logger.info("AI analyze requested (lang={}).", normalized);

        // Cache check happens BEFORE taking a fresh picture — the capture itself
        // is slow on the Pi Zero. Errors are never cached: a NOT_CONFIGURED or
        // UPSTREAM_ERROR result must let the next user click retry immediately.
        String cached = aiVisionCache.get(normalized);
        if (cached != null) {
            logger.info("AI analyze: cache hit for lang={}, returning {} chars.",
                    normalized, cached.length());
            return ResponseEntity.ok(okResponseBody(normalized, cached, true));
        }

        File picture;
        try {
            long t0 = System.currentTimeMillis();
            // Cacheable variant — if the Webcam page just loaded the snapshot,
            // the analyze button reuses the same file instead of firing the
            // camera again. Reduces light flashes + halves the wait on Pi Zero.
            picture = cameraService.takePictureCached(true, false);
            logger.info("AI analyze: snapshot obtained in {} ms, file={} ({} bytes).",
                    System.currentTimeMillis() - t0, picture.getAbsolutePath(), picture.length());
        } catch (IOException | InterruptedException e) {
            logger.warn("AI analyze: failed to capture snapshot.", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Technical detail already captured in the warn log above.
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "CAMERA_ERROR",
                    "The snapshot could not be captured. Please try again.", normalized);
        }

        try {
            String content = aiVisionClient.analyze(picture, normalized);
            aiVisionCache.put(normalized, content);
            logger.info("AI analyze: success, {} chars returned for lang={}.",
                    content.length(), normalized);
            return ResponseEntity.ok(okResponseBody(normalized, content, false));
        } catch (AiVisionException e) {
            HttpStatus status = "NOT_CONFIGURED".equals(e.getCode())
                    ? HttpStatus.NOT_IMPLEMENTED
                    : HttpStatus.BAD_GATEWAY;
            // Technical detail stays in the server log via getMessage();
            // the operator-facing response surfaces only the public message.
            logger.warn("AI analyze: failed (code={}, status={}, message={}).",
                    e.getCode(), status.value(), e.getMessage());
            return errorResponse(status, e.getCode(), e.getPublicMessage(), normalized);
        }
    }

    private static java.util.Map<String, String> okResponseBody(String lang, String content, boolean fromCache) {
        java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("status", "ok");
        body.put("lang", lang);
        body.put("message", content);
        body.put("cached", String.valueOf(fromCache));
        return body;
    }

    private static String normalizeLang(String lang) {
        if (lang == null) {
            return "en";
        }
        String code = lang.trim().toLowerCase();
        if (code.startsWith("fr")) return "fr";
        if (code.startsWith("ro")) return "ro";
        return "en";
    }

    private static ResponseEntity<java.util.Map<String, String>> errorResponse(
            HttpStatus status, String errorCode, String message, String lang) {
        java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("status", errorCode);
        body.put("lang", lang);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
