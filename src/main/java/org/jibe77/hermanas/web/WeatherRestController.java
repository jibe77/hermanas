package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.jibe77.hermanas.security.ssrf.DestinationValidator;
import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic endpoints for the OpenWeather integration. The "test" endpoint is
 * meant to be hit from the admin UI before saving a new URL / key combo, so the
 * operator gets immediate feedback on whether the credentials work without
 * persisting them first.
 */
@Lazy
@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather", description = "Diagnostics for the external weather provider")
public class WeatherRestController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherRestController.class);

    private final ConfigService configService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final DestinationValidator destinationValidator;

    public WeatherRestController(ConfigService configService,
                                 RestTemplateBuilder restTemplateBuilder,
                                 DestinationValidator destinationValidator) {
        this.configService = configService;
        this.restTemplateBuilder = restTemplateBuilder;
        this.destinationValidator = destinationValidator;
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
     *
     * <h3>SSRF-VULN-01</h3>
     * <p>This endpoint was the strongest SSRF oracle in the application: an ADMIN session
     * handed the server an arbitrary {@code http(s)} URL, and the answer came back as the
     * first 200 characters of the response body plus Java's own root-cause exception
     * string — in-band read <em>and</em> a port-scanning error channel, unthrottled. Three
     * things changed:</p>
     * <ul>
     *   <li>the destination goes through {@link DestinationValidator} before the socket
     *       opens. That also removes the {@code {key}} URI-template expansion for any host
     *       the operator has not explicitly allowlisted, which used to transmit the weather
     *       API key in cleartext to whatever host the caller named;</li>
     *   <li>the response is a fixed {@code ok}/{@code error} shape. No body excerpt, no
     *       {@code getMostSpecificCause().getMessage()} — the error text was itself a
     *       scanning channel. Failures are logged server-side instead, where the operator
     *       can still read them;</li>
     *   <li>{@link RateLimited} caps how often it can be dialed.</li>
     * </ul>
     */
    @Operation(summary = "Test an OpenWeather configuration without saving it")
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimited(maxRequests = 5, windowSeconds = 300,
            message = "Too many weather diagnostics in a row. Try again in a few minutes.")
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

        try {
            destinationValidator.validateHttpUrl(url, DestinationValidator.Sink.DIAGNOSTIC);
        } catch (RuntimeException refused) {
            // Fixed shape: the caller learns the request was refused, nothing about why
            // in a way that maps back to an internal host.
            logger.warn("Weather config test refused: {}", refused.getMessage());
            response.put("ok", false);
            response.put("error", "DESTINATION_REFUSED");
            response.put("message", "That destination is not allowed by the outbound policy.");
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
            // Deliberately nothing else. The previous `snippet` of the first 200 characters
            // turned this endpoint into an arbitrary internal-URL reader; the operator only
            // needs to know the provider answered.
            logger.info("Weather config test succeeded in {} ms ({} bytes of payload).",
                    elapsedMs, rawJson == null ? 0 : rawJson.length());
            return ResponseEntity.ok(response);
        } catch (RestClientException e) {
            // The cause chain is logged, never echoed: "Connection refused" versus "connect
            // timed out" versus a TLS alert is a complete port scanner on its own.
            logger.info("Weather config test failed: {}", e.getMessage(), e);
            response.put("ok", false);
            response.put("error", "PROVIDER_ERROR");
            response.put("message", "The provider could not be reached. See the server log for the cause.");
            return ResponseEntity.ok(response);
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
