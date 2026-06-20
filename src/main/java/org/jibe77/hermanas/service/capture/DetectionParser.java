package org.jibe77.hermanas.service.capture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the trailing detection JSON block emitted by the vision model and
 * returns both the cleaned-up human text and the parsed bounding boxes.
 *
 * <p>The prompt asks the model to append a fenced JSON block of the form
 * <pre>
 * ```json
 * {"detections":[{"type":"chicken","confidence":0.92,"box":[x,y,w,h]}, ...]}
 * ```
 * </pre>
 * at the very end of its answer. This parser locates that block, strips it
 * from the displayed text, and decodes it into {@link DetectionDto} instances.
 * </p>
 *
 * <p>Robustness over strictness: a missing block, an unfenced JSON tail or
 * malformed coordinates are all logged at WARN and produce an empty
 * detections list — the human text is still surfaced to the user. The overlay
 * is best-effort; the textual analysis is the load-bearing output.</p>
 */
@Component
public class DetectionParser {

    private static final Logger logger = LoggerFactory.getLogger(DetectionParser.class);

    // Three progressively looser anchors. Tried in order; the first one that
    // matches anywhere in the text wins, and we split THERE — keeping anything
    // before as human prose, stripping everything from the match onwards.
    //
    // 1. ```json     — markdown fence with the language tag (case-insensitive)
    // 2. ```         — markdown fence without the language tag
    // 3. {"detections" — the model forgot the fence entirely
    //
    // We deliberately do NOT use {@code while(find())} to pick the last match:
    // for ``` json\n\n{"detections"..., that loop would land on the {"detections"
    // occurrence and leave the fence opener in the human text. The first ```json
    // is the load-bearing anchor.
    private static final Pattern FENCE_JSON = Pattern.compile("(?si)```\\s*json\\b");
    private static final Pattern FENCE_BARE = Pattern.compile("```\\s*\\R");
    private static final Pattern OBJECT_OPEN = Pattern.compile("\\{\\s*\"detections\"");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Splits the raw model output into a {@link Parsed#humanText() human text}
     * (free of any JSON block) and a {@link Parsed#detections() detections}
     * list. Never throws — failures degrade to an empty list and the original
     * text minus any obvious trailing fence.
     */
    public Parsed parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new Parsed("", Collections.emptyList());
        }
        String trimmed = raw.trim();

        int splitAt = firstMatch(trimmed, FENCE_JSON);
        if (splitAt < 0) {
            splitAt = firstMatch(trimmed, FENCE_BARE);
        }
        if (splitAt < 0) {
            splitAt = firstMatch(trimmed, OBJECT_OPEN);
        }

        if (splitAt < 0) {
            return new Parsed(trimmed, Collections.emptyList());
        }

        String humanText = stripTrailingPunctuation(trimmed.substring(0, splitAt).trim());
        String tail = trimmed.substring(splitAt);
        String jsonPayload = extractJsonObject(tail);

        if (jsonPayload == null) {
            // Could not find a balanced JSON object — strip the fence anyway
            // so the operator does not see the ```json opener in the result.
            return new Parsed(humanText, Collections.emptyList());
        }

        List<DetectionDto> detections = decode(jsonPayload);
        return new Parsed(humanText, detections);
    }

    private static int firstMatch(String s, Pattern p) {
        Matcher m = p.matcher(s);
        return m.find() ? m.start() : -1;
    }

    /**
     * Returns the first balanced JSON object found in {@code s}, scanning from
     * the opening brace and tracking nesting depth. Tolerant of trailing
     * garbage (closing fence, prose, etc.) after the object — the scan stops
     * at the matching closing brace.
     */
    private static String extractJsonObject(String s) {
        int start = s.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Trims stray punctuation that the model sometimes leaves dangling at the
     * end of the prose right before the fence — most often a lone period after
     * the last bullet. Without this the rendered analysis ends on a chopped
     * sentence that looks broken.
     */
    private static String stripTrailingPunctuation(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c == ':' || c == '`' || c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                end--;
            } else {
                break;
            }
        }
        return s.substring(0, end);
    }

    private List<DetectionDto> decode(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode array = root.get("detections");
            if (array == null || !array.isArray()) {
                logger.warn("Detection JSON has no 'detections' array: {}",
                        truncate(json, 200));
                return Collections.emptyList();
            }
            List<DetectionDto> out = new ArrayList<>(array.size());
            for (JsonNode node : array) {
                DetectionDto dto = toDetection(node);
                if (dto != null) {
                    out.add(dto);
                }
            }
            return out;
        } catch (JsonProcessingException e) {
            logger.warn("Detection JSON could not be parsed ({}): {}",
                    e.getOriginalMessage(), truncate(json, 200));
            return Collections.emptyList();
        }
    }

    private DetectionDto toDetection(JsonNode node) {
        String type = node.path("type").asText("").trim().toLowerCase();
        if (type.isEmpty()) {
            return null;
        }
        double confidence = clamp01(node.path("confidence").asDouble(0.5));
        JsonNode box = node.get("box");
        if (box == null || !box.isArray() || box.size() < 4) {
            return null;
        }
        double x = clamp01(box.get(0).asDouble(Double.NaN));
        double y = clamp01(box.get(1).asDouble(Double.NaN));
        double w = clamp01(box.get(2).asDouble(Double.NaN));
        double h = clamp01(box.get(3).asDouble(Double.NaN));
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(w) || Double.isNaN(h)) {
            return null;
        }
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new DetectionDto(type, confidence, x, y, w, h);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return Double.NaN;
        }
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Result of splitting the raw model output. */
    public static final class Parsed {
        private final String humanText;
        private final List<DetectionDto> detections;

        public Parsed(String humanText, List<DetectionDto> detections) {
            this.humanText = humanText;
            this.detections = detections;
        }

        public String humanText() { return humanText; }
        public List<DetectionDto> detections() { return detections; }
    }
}
