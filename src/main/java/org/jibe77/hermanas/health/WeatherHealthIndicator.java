package org.jibe77.hermanas.health;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import static org.jibe77.hermanas.client.weather.WeatherClient.DEFAULT_VALUE_IF_DISABLED;

@Component
public class WeatherHealthIndicator implements HealthIndicator {

    WeatherClient weatherClient;

    public WeatherHealthIndicator(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @Override
    public Health health() {
        WeatherInfo weatherInfo = weatherClient.getInfo();
        if (weatherInfo.getHumidity() > DEFAULT_VALUE_IF_DISABLED && weatherInfo.getTemp() > DEFAULT_VALUE_IF_DISABLED) {
            return Health.up().build();
        } else {
            return Health.down().build();
        }
    }
}
