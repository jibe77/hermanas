package org.jibe77.hermanas.client.ai;

import org.jibe77.hermanas.service.capture.DetectionDto;
import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-language in-memory cache for successful AI analysis results.
 *
 * <p>The webcam page is opened by every visitor (including the recruiter demo)
 * and is now configured to auto-trigger an analysis on mount. Without a cache
 * each page open would burn 20+ seconds of GPU on Alyssa; with a 2-minute TTL
 * the LLM is hit at most once per language per window, and re-renders are
 * instant.</p>
 *
 * <p>The TTL is read from {@link ConfigService} on every call so the admin UI
 * can adjust it at runtime without restarting the app. Failures are
 * intentionally <strong>not</strong> cached: if the inference server is down
 * or the configuration is missing, we want the next user click to retry. Only
 * successful responses live in the map.</p>
 */
@Component
public class AiVisionCache {

    private static final Logger logger = LoggerFactory.getLogger(AiVisionCache.class);

    private final ConfigService configService;
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public AiVisionCache(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Returns the cached analysis (text + detections) for the given language,
     * or {@code null} when nothing is cached, the cached entry has expired, or
     * the call has been disabled by setting the TTL to a non-positive value.
     */
    public Entry get(String lang) {
        long ttlMs = configService.getAiInferenceCacheTtlMs();
        if (ttlMs <= 0) {
            return null;
        }
        Entry e = store.get(lang);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() - e.timestamp > ttlMs) {
            // Lazy eviction — cheaper than a scheduled job for a 1-3 entry map.
            store.remove(lang, e);
            return null;
        }
        return e;
    }

    /** Records a successful analysis. Overwrites any previous entry for the language. */
    public void put(String lang, String body, List<DetectionDto> detections) {
        if (configService.getAiInferenceCacheTtlMs() <= 0 || body == null) {
            return;
        }
        List<DetectionDto> safe = detections == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(detections);
        store.put(lang, new Entry(body, safe, System.currentTimeMillis()));
        logger.debug("Cached AI analysis for lang={} ({} chars, {} detections).",
                lang, body.length(), safe.size());
    }

    /** Manually clears the cache — exposed for test purposes and for an eventual admin button. */
    public void clear() {
        store.clear();
    }

    /** Immutable snapshot of a cached analysis. */
    public static final class Entry {
        private final String body;
        private final List<DetectionDto> detections;
        final long timestamp;

        Entry(String body, List<DetectionDto> detections, long timestamp) {
            this.body = body;
            this.detections = detections;
            this.timestamp = timestamp;
        }

        public String getBody() { return body; }
        public List<DetectionDto> getDetections() { return detections; }
    }
}
