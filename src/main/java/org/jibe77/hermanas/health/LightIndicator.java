package org.jibe77.hermanas.health;

import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
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
        if (StatusEnum.ON.equals(lightService.getStatus().getStatusEnum())) {
            lightService.switchOff();
            if (StatusEnum.ON.equals(lightService.getStatus().getStatusEnum())) {
               return Health.down().build();
            } else {
                lightService.switchOn();
                return Health.up().build();
            }
        } else {
            lightService.switchOn();
            if (StatusEnum.ON.equals(lightService.getStatus().getStatusEnum())) {
                lightService.switchOff();
                return Health.up().build();
            } else {
                return Health.down().build();
            }
        }
    }
}
