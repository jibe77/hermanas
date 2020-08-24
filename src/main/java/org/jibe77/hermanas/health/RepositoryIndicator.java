package org.jibe77.hermanas.health;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RepositoryIndicator implements HealthIndicator {

    SensorRepository sensorRepository;

    public RepositoryIndicator(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Override
    public Health health() {
        Optional<Sensor> s = sensorRepository.findById(-1);
        if (!s.isPresent()) {
            return Health.up().build();
        } else {
            return Health.down().build();
        }
    }
}
