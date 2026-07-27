package org.jibe77.hermanas.health;

import org.jibe77.hermanas.service.door.model.DoorStatus;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.jibe77.hermanas.web.DoorRestController;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for door physical position.
 * Checks if the door's button sensors are working correctly and position is determinable.
 *
 * <p>Health is UP if:</p>
 * <ul>
 *   <li>Door is fully OPENED (top button pressed)</li>
 *   <li>Door is fully CLOSED (bottom button pressed)</li>
 * </ul>
 *
 * <p>Health is DOWN if:</p>
 * <ul>
 *   <li>Door position is UNDEFINED (no buttons pressed, no history)</li>
 *   <li>Door position is uncertain (SEEMS_OPENED, SEEMS_CLOSED)</li>
 * </ul>
 *
 * @see DoorRestController
 * @see DoorStatus
 */
@Component
public class DoorIndicator implements HealthIndicator {

    private final DoorRestController doorRestController;

    public DoorIndicator(DoorRestController doorRestController) {
        this.doorRestController = doorRestController;
    }

    @Override
    public Health health() {
        try {
            DoorStatus doorStatus = doorRestController.statusInfo();
            DoorStatusEnum status = doorStatus.getStatus();

            // Health is UP only if door is in a definite position
            if (status == DoorStatusEnum.OPENED || status == DoorStatusEnum.CLOSED) {
                return Health.up()
                        .withDetail("status", status.name())
                        .withDetail("last_action", doorStatus.getTimeStatusHasChanged())
                        .build();
            }

            // Health is DOWN for uncertain or undefined positions
            return Health.down()
                    .withDetail("status", status.name())
                    .withDetail("last_action", doorStatus.getTimeStatusHasChanged())
                    .withDetail("reason", "Door position unclear - buttons may not be working correctly")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("exception", e.getMessage())
                    .withDetail("reason", "Failed to read door status")
                    .build();
        }
    }
}
