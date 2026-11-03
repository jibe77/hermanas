package org.jibe77.hermanas.client.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class WeatherClient {

    /**
     * Échecs consécutifs au-delà desquels l'API n'est plus appelée pendant {@link #PAUSE}.
     * Remplace le circuit breaker resilience4j : un appel par minute au plus et la météo
     * désactivée par défaut — un compteur suffit, sans module ni thread supplémentaire.
     */
    static final int FAILURE_THRESHOLD = 3;
    static final Duration PAUSE = Duration.ofMinutes(5);

    private final org.jibe77.hermanas.service.config.ConfigService configService;

    public static final Double DEFAULT_VALUE_IF_DISABLED = -100d;

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    final RestTemplateBuilder builder;

    private int consecutiveFailures;
    private Instant pausedUntil = Instant.MIN;

    public WeatherClient(RestTemplateBuilder builder,
                         org.jibe77.hermanas.service.config.ConfigService configService) {
        this.builder = builder;
        this.configService = configService;
    }

    public synchronized WeatherInfo getInfo() {
        if (!configService.isWeatherInfoEnabled()) {
            return getDefaultWeatherInfo();
        }
        if (Instant.now().isBefore(pausedUntil)) {
            log.debug("Weather API paused until {}; returning default info.", pausedUntil);
            return getDefaultWeatherInfo();
        }
        try {
            WeatherInfo weatherInfo = builder.build().getForObject(
                    configService.getWeatherInfoUrl(),
                    WeatherInfo.class,
                    configService.getLatitude(),
                    configService.getLongitude(),
                    configService.getWeatherInfoKey());
            consecutiveFailures = 0;
            log.info("Weather info content : {}", weatherInfo);
            return weatherInfo;
        } catch (RuntimeException ex) {
            if (++consecutiveFailures >= FAILURE_THRESHOLD) {
                consecutiveFailures = 0;
                pausedUntil = Instant.now().plus(PAUSE);
                log.warn("Weather API unavailable ({}); {} failures in a row, pausing calls for {} min.",
                        ex.getMessage(), FAILURE_THRESHOLD, PAUSE.toMinutes());
            } else {
                log.warn("Weather API unavailable ({}); returning default info.", ex.getMessage());
            }
            return getDefaultWeatherInfo();
        }
    }

    private WeatherInfo getDefaultWeatherInfo() {
        // default value if disabled or fallback triggered.
        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setValues(DEFAULT_VALUE_IF_DISABLED, DEFAULT_VALUE_IF_DISABLED);
        return weatherInfo;
    }

    void setWeatherInfoEnabled(boolean weatherInfoEnabled) {
        if (configService != null) {
            configService.setWeatherInfoEnabled(weatherInfoEnabled);
        }
    }
}
