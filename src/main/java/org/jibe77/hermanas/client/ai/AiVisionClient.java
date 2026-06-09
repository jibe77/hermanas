package org.jibe77.hermanas.client.ai;

import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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
    private final RestTemplate restTemplate;

    public AiVisionClient(ConfigService configService, RestTemplateBuilder builder) {
        this.configService = configService;
        // Vision calls on a Pi-class CPU can legitimately take 30+ seconds; give
        // the request a generous window before timing out.
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * Sends {@code image} along with the chicken-coop analysis prompt to the
     * configured inference server. Returns the trimmed text content of the
     * first choice. Caller is responsible for cleaning up {@code image}.
     */
    public String analyze(File image, String lang) throws AiVisionException {
        String baseUrl = configService.getAiInferenceUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new AiVisionException("NOT_CONFIGURED",
                    "AI inference URL is not configured.");
        }
        String model = configService.getAiInferenceModel();
        String url = chatCompletionsUrl(baseUrl.trim());

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(image.toPath()));
        } catch (IOException e) {
            throw new AiVisionException("IMAGE_READ_FAILED",
                    "Could not read the snapshot file: " + e.getMessage());
        }

        Map<String, Object> payload = buildChatPayload(model, lang, base64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            long elapsed = System.currentTimeMillis() - start;
            logger.info("AI vision call to {} returned in {} ms (model={}, lang={}).",
                    url, elapsed, model, lang);
            return extractContent(response);
        } catch (RestClientException e) {
            logger.warn("AI vision call to {} failed: {}", url, e.getMessage());
            throw new AiVisionException("UPSTREAM_ERROR",
                    "Inference server unreachable or returned an error: " + e.getMessage());
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
    private Map<String, Object> buildChatPayload(String model, String lang, String base64Image) {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", CameraPromptBuilder.buildPrompt(lang));

        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        List<Map<String, Object>> content = new ArrayList<>(2);
        content.add(textPart);
        content.add(imagePart);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(message));
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
