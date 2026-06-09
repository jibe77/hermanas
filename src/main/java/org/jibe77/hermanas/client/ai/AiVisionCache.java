package org.jibe77.hermanas.client.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * <p>Failures are intentionally <strong>not</strong> cached: if the inference
 * server is down or the configuration is missing, we want the next user click
 * to retry. Only successful responses live in the map.</p>
 */
@Component
public class AiVisionCache {

    private static final Logger logger = LoggerFactory.getLogger(AiVisionCache.class);

    /** Cache TTL, in milliseconds. Two minutes by default — see application.properties. */
    @Value("${ai.inference.cache.ttl-ms:120000}")
    private long ttlMs;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * Returns the cached analysis text for the given language, or {@code null}
     * when nothing is cached, the cached entry has expired, or the call has
     * been disabled by setting {@code ttlMs} to a non-positive value.
     */
    public String get(String lang) {
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
        return e.body;
    }

    /** Records a successful analysis. Overwrites any previous entry for the language. */
    public void put(String lang, String body) {
        if (ttlMs <= 0 || body == null) {
            return;
        }
        store.put(lang, new Entry(body, System.currentTimeMillis()));
        logger.debug("Cached AI analysis for lang={} ({} chars).", lang, body.length());
    }

    /** Manually clears the cache — exposed for test purposes and for an eventual admin button. */
    public void clear() {
        store.clear();
    }

    private static final class Entry {
        final String body;
        final long timestamp;

        Entry(String body, long timestamp) {
            this.body = body;
            this.timestamp = timestamp;
        }
    }
}
