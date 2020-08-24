package org.jibe77.hermanas.client.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

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

    @Autowired
    RestTemplateBuilder builder;

    public WeatherInfo getInfo() {
        if (weatherInfoEnabled) {
            WeatherInfo weatherInfo = builder.build().getForObject(
                    weatherInfoUrl,
                    WeatherInfo.class,
                    latitude,
                    longitude,
                    weatherInfoKey);
            log.info("Weather info content : {}", weatherInfo);
            return weatherInfo;
        } else {
            // default value if disabled.
            WeatherInfo weatherInfo = new WeatherInfo();
            weatherInfo.setValues(DEFAULT_VALUE_IF_DISABLED, DEFAULT_VALUE_IF_DISABLED);
            return weatherInfo;
        }
    }
}
