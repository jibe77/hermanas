package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic endpoints for the OpenWeather integration. The "test" endpoint is
 * meant to be hit from the admin UI before saving a new URL / key combo, so the
 * operator gets immediate feedback on whether the credentials work without
 * persisting them first.
 */
@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather", description = "Diagnostics for the external weather provider")
public class WeatherRestController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherRestController.class);

    private final ConfigService configService;
    private final RestTemplateBuilder restTemplateBuilder;

    public WeatherRestController(ConfigService configService,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.configService = configService;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    /**
     * Probes the OpenWeather endpoint with the supplied URL/key (or the currently
     * stored ones when the request body is empty) and reports the outcome. Never
     * persists anything — the operator is expected to call the dedicated PUT
     * endpoints once they are happy with the result.
     *
     * <p>The body is intentionally minimal — a {@link TestRequest} with optional
     * url/key/latitude/longitude. Anything not provided falls back to the value
     * currently stored in {@link ConfigService}, so the admin UI can pre-test
     * "what is currently configured" with an empty body.</p>
     */
    @Operation(summary = "Test an OpenWeather configuration without saving it")
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConfig(@RequestBody(required = false) TestRequest body) {
        TestRequest req = body != null ? body : new TestRequest();
        String url = isBlank(req.getUrl()) ? configService.getWeatherInfoUrl() : req.getUrl().trim();
        String key = isBlank(req.getKey()) ? configService.getWeatherInfoKey() : req.getKey().trim();
        Double latitude = req.getLatitude() != null ? req.getLatitude() : configService.getLatitude();
        Double longitude = req.getLongitude() != null ? req.getLongitude() : configService.getLongitude();

        Map<String, Object> response = new HashMap<>();
        if (isBlank(url) || isBlank(key)) {
            response.put("ok", false);
            response.put("error", "MISSING_CONFIG");
            response.put("message", "URL and API key are required.");
            return ResponseEntity.ok(response);
        }

        RestTemplate rest = restTemplateBuilder.build();
        try {
            long start = System.currentTimeMillis();
            // Same template the production WeatherClient uses, so the same {latitude}/
            // {longitude}/{key} placeholders are interpolated.
            String rawJson = rest.getForObject(url, String.class, latitude, longitude, key);
            long elapsedMs = System.currentTimeMillis() - start;
            response.put("ok", true);
            response.put("durationMs", elapsedMs);
            // Include a short, safe excerpt of the body so the operator gets visual
            // confirmation. Truncated to avoid splattering the toast with kilobytes.
            response.put("snippet", rawJson != null && rawJson.length() > 200
                    ? rawJson.substring(0, 200) + "…"
                    : rawJson);
            return ResponseEntity.ok(response);
        } catch (RestClientException e) {
            logger.info("Weather config test failed: {}", e.getMessage());
            response.put("ok", false);
            response.put("error", "PROVIDER_ERROR");
            response.put("message", e.getMostSpecificCause() != null
                    ? e.getMostSpecificCause().getMessage()
                    : e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Optional override of the URL/key/latitude/longitude to test. */
    public static class TestRequest {
        private String url;
        private String key;
        private Double latitude;
        private Double longitude;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}
