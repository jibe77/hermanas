package org.jibe77.hermanas.client.ai;

import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client around the OpenAI-compatible /chat/completions endpoint exposed
 * by the local inference server (llama.cpp / Alyssa). Sends a multimodal
 * request — text + base64 JPEG image — and returns the model's textual reply
 * verbatim. Errors propagate as {@link AiVisionException} so the REST layer
 * can map them to a clean 5xx response.
 *
 * <p>The chicken-coop prompt is built in {@link CameraPromptBuilder} and
 * accepts a locale hint ({@code "en"} / {@code "fr"} / {@code "ro"}) so the
 * model answers in the user's UI language without us having to translate
 * server-side.</p>
 */
@Component
public class AiVisionClient {

    private static final Logger logger = LoggerFactory.getLogger(AiVisionClient.class);

    private final ConfigService configService;
    private final CameraPromptBuilder promptBuilder;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public AiVisionClient(ConfigService configService,
                          CameraPromptBuilder promptBuilder,
                          RestTemplateBuilder builder) {
        this.configService = configService;
        this.promptBuilder = promptBuilder;
        // Timeouts and retry policy are read from ConfigService so values
        // persisted from the Webcam configuration page survive across
        // reboots (application.properties only feeds first-boot defaults).
        // RestTemplate is built once at construction — changes from the UI
        // take effect on the next reboot, same as the camera settings.
        int connectTimeoutMs = configService.getAiInferenceConnectTimeoutMs();
        int readTimeoutMs = configService.getAiInferenceReadTimeoutMs();
        int maxAttempts = configService.getAiInferenceRetryMaxAttempts();
        long initialBackoffMs = configService.getAiInferenceRetryInitialBackoffMs();
        long maxBackoffMs = configService.getAiInferenceRetryMaxBackoffMs();
        logger.info("AI vision: connect={} ms, read={} ms, retry attempts={}, backoff {}-{} ms.",
                connectTimeoutMs, readTimeoutMs, maxAttempts, initialBackoffMs, maxBackoffMs);

        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();

        // Retry policy: only CONNECT-phase failures are retried. We refuse
        // to retry on a SocketTimeoutException post-connection because the
        // read timeout (~3 min) would then multiply by the attempt count —
        // a hung inference is unlikely to recover within seconds, and the
        // user would wait many minutes for the final failure. HTTP 4xx
        // (HttpClientErrorException) and 5xx (HttpServerErrorException) are
        // also not retried: they signal a request-level or server-level
        // problem that a quick retry won't fix.
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxAttempts,
                java.util.Collections.singletonMap(ResourceAccessException.class, true),
                true) {
            @Override
            public boolean canRetry(org.springframework.retry.RetryContext context) {
                Throwable t = context.getLastThrowable();
                if (t instanceof ResourceAccessException && isConnectFailure(t)) {
                    return super.canRetry(context);
                }
                if (t == null) {
                    return super.canRetry(context);
                }
                return false;
            }
        };

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(initialBackoffMs);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(maxBackoffMs);

        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setRetryPolicy(retryPolicy);
        this.retryTemplate.setBackOffPolicy(backOff);
    }

    /**
     * True when the cause chain of {@code t} indicates the request failed
     * to even establish a connection. Spring wraps both connect and read
     * timeouts in {@link ResourceAccessException}; we look one level deeper
     * to distinguish the two so we don't compound a multi-minute read
     * timeout across retries.
     */
    private static boolean isConnectFailure(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String name = cur.getClass().getSimpleName();
            if ("ConnectTimeoutException".equals(name)
                    || "HttpHostConnectException".equals(name)
                    || "ConnectException".equals(name)
                    || "NoRouteToHostException".equals(name)
                    || "UnknownHostException".equals(name)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * Sends {@code image} along with the chicken-coop analysis prompt to the
     * configured inference server. Returns the trimmed text content of the
     * first choice. Caller is responsible for cleaning up {@code image}.
     */
    public String analyze(File image, String lang) throws AiVisionException {
        return sendChatRequest(image, promptBuilder.buildPrompt(lang), lang);
    }

    /**
     * Variant of {@link #analyze} used by the morning/evening door-state
     * verification scheduler. Skips the full coop checklist and asks a single
     * yes/no question about the door in the bottom-left corner of the frame.
     */
    public String analyzeDoorState(File image, boolean isMorning) throws AiVisionException {
        return sendChatRequest(image, promptBuilder.buildDoorCheckPrompt(isMorning), "en");
    }

    private String sendChatRequest(File image, String userPrompt, String lang) throws AiVisionException {
        String baseUrl = configService.getAiInferenceUrl();
        String model = configService.getAiInferenceModel();
        // INFO-level so the operator can read the current configuration straight from
        // the log when something fails — particularly useful when ai.inference.url
        // looks empty even though it was set, which usually means the cached value
        // has not been refreshed yet.
        logger.info("AI vision request: baseUrl='{}' (length={}), model='{}', lang='{}', image={} ({} bytes).",
                baseUrl, baseUrl == null ? 0 : baseUrl.length(),
                model, lang, image.getAbsolutePath(), image.length());

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new AiVisionException("NOT_CONFIGURED",
                    "AI inference URL is not configured.");
        }
        String url = chatCompletionsUrl(baseUrl.trim());

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(image.toPath()));
        } catch (IOException e) {
            logger.warn("AI vision: failed to read snapshot file {}.", image.getAbsolutePath(), e);
            throw new AiVisionException("IMAGE_READ_FAILED",
                    "Could not read the snapshot file: " + e.getMessage(),
                    "The snapshot could not be read for analysis. Please try again.");
        }
        logger.debug("AI vision: encoded image to base64, payload size ~{} chars.", base64.length());

        Map<String, Object> payload = buildChatPayload(model, lang, base64, userPrompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                if (attempt > 1) {
                    logger.info("AI vision: retry attempt {} for POST {} (model={}).",
                            attempt, url, model);
                }
                return restTemplate.postForObject(url, entity, Map.class);
            });
            long elapsed = System.currentTimeMillis() - start;
            logger.info("AI vision: POST {} returned in {} ms (model={}, lang={}).",
                    url, elapsed, model, lang);
            return extractContent(response);
        } catch (RestClientException e) {
            logger.warn("AI vision: POST {} failed after retries (model={}): {}",
                    url, model, e.toString());
            throw new AiVisionException("UPSTREAM_ERROR",
                    "Inference server unreachable or returned an error: " + e.getMessage(),
                    "AI analysis is temporarily unavailable. Please try again later.");
        }
    }

    /**
     * Accepts a base URL with or without a trailing {@code /v1}, and with or
     * without a trailing slash. Always returns {@code <base>/chat/completions}.
     */
    static String chatCompletionsUrl(String baseUrl) {
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (b.endsWith("/chat/completions")) {
            return b;
        }
        return b + "/chat/completions";
    }

    /**
     * Builds the multimodal payload understood by llama.cpp's OpenAI-compatible
     * server. {@code temperature} is kept low so the answer to "how many
     * chickens" stays repeatable for a given snapshot.
     */
    private Map<String, Object> buildChatPayload(String model, String lang, String base64Image, String userPrompt) {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userPrompt);

        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        List<Map<String, Object>> content = new ArrayList<>(2);
        content.add(textPart);
        content.add(imagePart);

        // System message constrains the answer language. Vision models give system
        // messages significantly more steering weight than user-level hints, which
        // is what we need: a single language directive in the user prompt was
        // ignored about a third of the time on qwen2.5-vl.
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", promptBuilder.buildSystemInstruction(lang));

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(systemMessage, userMessage));
        payload.put("temperature", 0.2);
        payload.put("stream", false);
        return payload;
    }

    /**
     * Pulls the text out of an OpenAI-compatible response body. Throws when
     * the shape does not match — the upstream server is unlikely to return
     * something useful if this happens, so failing loudly is correct.
     */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) throws AiVisionException {
        if (response == null) {
            throw new AiVisionException("EMPTY_RESPONSE",
                    "Inference server returned an empty response.");
        }
        Object choices = response.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
            throw new AiVisionException("MALFORMED_RESPONSE",
                    "Inference response did not contain any choice.");
        }
        Map<String, Object> first = (Map<String, Object>) ((List<?>) choices).get(0);
        Object messageObj = first.get("message");
        if (!(messageObj instanceof Map)) {
            throw new AiVisionException("MALFORMED_RESPONSE",
                    "Inference response did not contain a message.");
        }
        Object content = ((Map<String, Object>) messageObj).get("content");
        if (!(content instanceof String)) {
            throw new AiVisionException("MALFORMED_RESPONSE",
                    "Inference response content is not a string.");
        }
        return ((String) content).trim();
    }
}
