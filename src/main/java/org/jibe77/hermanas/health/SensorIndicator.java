package org.jibe77.hermanas.health;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.service.SensorService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SensorIndicator implements HealthIndicator {

    SensorService sensorService;

    public SensorIndicator(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @Override
    public Health health() {
        try {
            Sensor sensor = sensorService.getInfo();
            if (sensor.getHumidity() < 100d
                    && sensor.getHumidity() > 0d
                    && sensor.getTemperature() < 60d
                    && sensor.getTemperature() > -50d) {
                return Health.up().build();
            }
        } catch (IOException e) {
            return Health.down().withDetail("exception", e.getMessage()).build();
        }
        return Health.down().build();
    }
}
