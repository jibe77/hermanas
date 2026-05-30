package org.jibe77.hermanas.client.weather;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
public class WeatherClient {

    public static final String CIRCUIT_BREAKER_NAME = "weatherApi";

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    private final org.jibe77.hermanas.service.config.ConfigService configService;

    public static final Double DEFAULT_VALUE_IF_DISABLED = -100d;

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    final RestTemplateBuilder builder;

    public WeatherClient(RestTemplateBuilder builder,
                         org.jibe77.hermanas.service.config.ConfigService configService) {
        this.builder = builder;
        this.configService = configService;
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getInfoFallback")
    public WeatherInfo getInfo() {
        if (!configService.isWeatherInfoEnabled()) {
            return getDefaultWeatherInfo();
        }
        WeatherInfo weatherInfo = builder.build().getForObject(
                configService.getWeatherInfoUrl(),
                WeatherInfo.class,
                latitude,
                longitude,
                configService.getWeatherInfoKey());
        log.info("Weather info content : {}", weatherInfo);
        return weatherInfo;
    }

    @SuppressWarnings("unused") // referenced by @CircuitBreaker fallbackMethod
    private WeatherInfo getInfoFallback(Throwable ex) {
        log.warn("Weather API unavailable ({}); returning default info.", ex.getMessage());
        return getDefaultWeatherInfo();
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
