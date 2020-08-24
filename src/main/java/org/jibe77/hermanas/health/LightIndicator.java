package org.jibe77.hermanas.health;

import org.jibe77.hermanas.service.LightService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LightIndicator implements HealthIndicator {

    LightService lightService;

    public LightIndicator(LightService lightService) {
        this.lightService = lightService;
    }

    @Override
    public Health health() {
        if (lightService.isSwitchedOn()) {
            lightService.switchOff();
            if (lightService.isSwitchedOn()) {
               return Health.down().build();
            } else {
                lightService.switchOn();
                return Health.up().build();
            }
        } else {
            lightService.switchOn();
            if (lightService.isSwitchedOn()) {
                lightService.switchOff();
                return Health.up().build();
            } else {
                return Health.down().build();
            }
        }
    }
}
