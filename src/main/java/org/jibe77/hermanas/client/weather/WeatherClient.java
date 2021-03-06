package org.jibe77.hermanas.client.weather;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class WeatherClient {

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

    public WeatherInfo getInfo() {
        if (weatherInfoEnabled) {
            try {
                WeatherInfo weatherInfo = builder.build().getForObject(
                        weatherInfoUrl,
                        WeatherInfo.class,
                        latitude,
                        longitude,
                        weatherInfoKey);
                log.info("Weather info content : {}", weatherInfo);
                return weatherInfo;
            } catch (ResourceAccessException e) {
                log.error("Can't process weather info request.", e);
                return getDefaultWeatherInfo();
            }
        } else {
            return getDefaultWeatherInfo();
        }
    }

    private WeatherInfo getDefaultWeatherInfo() {
        // default value if disabled.
        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setValues(DEFAULT_VALUE_IF_DISABLED, DEFAULT_VALUE_IF_DISABLED);
        return weatherInfo;
    }

    void setWeatherInfoEnabled(boolean weatherInfoEnabled) {
        this.weatherInfoEnabled = weatherInfoEnabled;
    }
}
