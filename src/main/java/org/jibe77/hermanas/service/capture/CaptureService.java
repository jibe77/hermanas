package org.jibe77.hermanas.service.capture;

import org.jibe77.hermanas.client.ai.AiVisionCache;
import org.jibe77.hermanas.client.ai.AiVisionClient;
import org.jibe77.hermanas.client.ai.AiVisionException;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.websocket.CaptureNotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous orchestrator for the "take a picture + analyze it" pipeline.
 *
 * <p>Replaces the previous two-step flow where the SPA first hit
 * {@code GET /camera/takePicture} (which could spend 30 s on the Pi and time
 * out at the reverse proxy) and then {@code GET /camera/analyze}. Both
 * operations now run in the background; the client kicks the job, polls a
 * status endpoint, and streams the image as soon as it is available — no
 * single HTTP request ever needs to stay open long enough to be killed by
 * Caddy.</p>
 *
 * <p>Job storage is an in-memory map: this is a single-instance app on a
 * Pi Zero, persistence buys us nothing. A {@link Scheduled} sweep purges
 * stale entries so the map cannot grow unbounded.</p>
 */
@Service
public class CaptureService {

    private static final Logger logger = LoggerFactory.getLogger(CaptureService.class);

    /** Max age of a finished/errored capture in the in-memory store. */
    static final long TTL_MS = 10 * 60 * 1000L;

    private final Map<String, CaptureState> captures = new ConcurrentHashMap<>();

    private final CameraService cameraService;
    private final AiVisionClient aiVisionClient;
    private final AiVisionCache aiVisionCache;
    private final DetectionParser detectionParser;

    /**
     * STOMP publisher used to push state transitions to the SPA. Optional so the
     * service stays trivially testable in unit tests that do not wire the
     * WebSocket layer; in production it is always provided by Spring.
     */
    @Autowired(required = false)
    private CaptureNotificationController notifier;

    public CaptureService(CameraService cameraService,
                          AiVisionClient aiVisionClient,
                          AiVisionCache aiVisionCache,
                          DetectionParser detectionParser) {
        this.cameraService = cameraService;
        this.aiVisionClient = aiVisionClient;
        this.aiVisionCache = aiVisionCache;
        this.detectionParser = detectionParser;
    }

    /**
     * Registers a new capture job, kicks the background pipeline and returns
     * the opaque id the client should poll. The whole side-effect chain runs
     * inside {@link #runPipeline(String, String)} via {@code @Async} so this
     * method itself returns immediately.
     */
    public String startAsync(String lang) {
        String normalized = normalizeLang(lang);
        String id = UUID.randomUUID().toString();
        captures.put(id, new CaptureState(normalized));
        logger.info("Capture {} started (lang={}).", id, normalized);
        runPipeline(id, normalized);
        return id;
    }

    @Async
    void runPipeline(String id, String lang) {
        CaptureState state = captures.get(id);
        if (state == null) {
            return;
        }
        File picture;
        try {
            long t0 = System.currentTimeMillis();
            picture = cameraService.takePictureCached(true, false);
            long elapsed = System.currentTimeMillis() - t0;
            logger.info("Capture {} took picture in {} ms ({} bytes).",
                    id, elapsed, picture.length());
            byte[] image = Files.readAllBytes(picture.toPath());
            state.setImage(image);
            state.setStatus(CaptureStatus.ANALYZING);
            logger.info("Capture {} transition CAPTURING -> ANALYZING after {} ms.",
                    id, System.currentTimeMillis() - state.getCreatedAt());
            publish(id, state);
        } catch (IOException | InterruptedException e) {
            logger.warn("Capture {} failed to take picture.", id, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Operator-facing message stays neutral; technical detail is
            // already in the preceding logger.warn("...", id, e) call.
            state.fail("CAMERA_ERROR", "The snapshot could not be captured. Please try again.");
            publish(id, state);
            return;
        }

        AiVisionCache.Entry cached = aiVisionCache.get(lang);
        if (cached != null) {
            logger.info("Capture {} got cached analysis ({} chars, {} detections).",
                    id, cached.getBody().length(), cached.getDetections().size());
            state.complete(cached.getBody(), cached.getDetections());
            publish(id, state);
            return;
        }

        try {
            String raw = aiVisionClient.analyze(picture, lang);
            DetectionParser.Parsed parsed = detectionParser.parse(raw);
            aiVisionCache.put(lang, parsed.humanText(), parsed.detections());
            logger.info("Capture {} analysis done ({} chars, {} detections).",
                    id, parsed.humanText().length(), parsed.detections().size());
            state.complete(parsed.humanText(), parsed.detections());
        } catch (AiVisionException e) {
            logger.warn("Capture {} analysis failed (code={}, msg={}).",
                    id, e.getCode(), e.getMessage());
            state.fail(e.getCode(), e.getPublicMessage());
        }
        publish(id, state);
    }

    /**
     * Pushes the current state of a capture on its dedicated STOMP topic so the
     * SPA can react without polling. No-op when the notifier is not wired (unit
     * tests instantiating CaptureService by hand).
     */
    private void publish(String id, CaptureState state) {
        if (notifier != null) {
            notifier.notify(id, state.toDto());
        }
    }

    /**
     * Returns the captured JPEG bytes when available. Empty during the
     * {@code CAPTURING} phase, present from {@code ANALYZING} onwards.
     */
    public Optional<byte[]> getImage(String id) {
        CaptureState state = captures.get(id);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.getImage());
    }

    public Optional<CaptureStateDto> getStatus(String id) {
        CaptureState state = captures.get(id);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state.toDto());
    }

    /**
     * Drops entries older than {@link #TTL_MS}. Runs every minute. Lightweight
     * enough for the Pi Zero — the map only ever holds a handful of rows for a
     * single visitor.
     */
    @Scheduled(fixedDelay = 60_000L)
    void purgeStale() {
        long cutoff = System.currentTimeMillis() - TTL_MS;
        Iterator<Map.Entry<String, CaptureState>> it = captures.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            Map.Entry<String, CaptureState> entry = it.next();
            if (entry.getValue().getCreatedAt() < cutoff) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            logger.debug("Purged {} stale capture(s); {} remaining.", removed, captures.size());
        }
    }

    private static String normalizeLang(String lang) {
        if (lang == null) {
            return "en";
        }
        String n = lang.trim().toLowerCase();
        return ("fr".equals(n) || "ro".equals(n)) ? n : "en";
    }

    /** Internal mutable state for a capture job. Held inside the service map. */
    private static final class CaptureState {
        private final String lang;
        private final long createdAt = System.currentTimeMillis();
        private volatile CaptureStatus status = CaptureStatus.CAPTURING;
        private volatile byte[] image;
        private volatile String message;
        private volatile String errorCode;
        private volatile String errorMessage;
        private volatile List<DetectionDto> detections = Collections.emptyList();

        CaptureState(String lang) {
            this.lang = lang;
        }

        void setImage(byte[] image) { this.image = image; }
        void setStatus(CaptureStatus status) { this.status = status; }
        byte[] getImage() { return image; }
        long getCreatedAt() { return createdAt; }

        void complete(String content, List<DetectionDto> detections) {
            this.message = content;
            this.detections = detections == null ? Collections.emptyList() : detections;
            this.status = CaptureStatus.DONE;
        }

        void fail(String code, String msg) {
            this.errorCode = code;
            this.errorMessage = msg;
            this.status = CaptureStatus.ERROR;
        }

        CaptureStateDto toDto() {
            return new CaptureStateDto(status, lang, message, errorCode, errorMessage,
                    image != null, detections);
        }
    }
}
