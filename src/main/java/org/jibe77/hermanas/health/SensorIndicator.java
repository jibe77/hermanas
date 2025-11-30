package org.jibe77.hermanas.health;

import org.jibe77.hermanas.dto.SensorDTO;
import org.jibe77.hermanas.service.SensorRestController;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SensorIndicator implements HealthIndicator {

    SensorRestController sensorRestController;

    public SensorIndicator(SensorRestController sensorRestController) {
        this.sensorRestController = sensorRestController;
    }

    @Override
    public Health health() {
        try {
            SensorDTO sensor = sensorRestController.getInfo();
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
