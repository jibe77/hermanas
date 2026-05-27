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

    @Value("${weather.info.url}")
    public String weatherInfoUrl;

    @Value("${weather.info.key}")
    public String weatherInfoKey;

    @Value("${weather.info.enabled}")
    public boolean weatherInfoEnabled;

    public static final Double DEFAULT_VALUE_IF_DISABLED = -100d;

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    final RestTemplateBuilder builder;

    public WeatherClient(RestTemplateBuilder builder) {
        this.builder = builder;
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getInfoFallback")
    public WeatherInfo getInfo() {
        if (!weatherInfoEnabled) {
            return getDefaultWeatherInfo();
        }
        WeatherInfo weatherInfo = builder.build().getForObject(
                weatherInfoUrl,
                WeatherInfo.class,
                latitude,
                longitude,
                weatherInfoKey);
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
        this.weatherInfoEnabled = weatherInfoEnabled;
    }
}
