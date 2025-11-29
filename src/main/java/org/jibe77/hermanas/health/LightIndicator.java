package org.jibe77.hermanas.health;

import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.LightRestController;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LightIndicator implements HealthIndicator {

    LightRestController lightRestController;

    public LightIndicator(LightRestController lightRestController) {
        this.lightRestController = lightRestController;
    }

    @Override
    public Health health() {
        if (StatusEnum.ON.equals(lightRestController.getStatus().getStatusEnum())) {
            lightRestController.switcher(false);
            if (StatusEnum.ON.equals(lightRestController.getStatus().getStatusEnum())) {
               return Health.down().build();
            } else {
                lightRestController.switcher(true);
                return Health.up().build();
            }
        } else {
            lightRestController.switcher(true);
            if (StatusEnum.ON.equals(lightRestController.getStatus().getStatusEnum())) {
                lightRestController.switcher(false);
                return Health.up().build();
            } else {
                return Health.down().build();
            }
        }
    }
}
