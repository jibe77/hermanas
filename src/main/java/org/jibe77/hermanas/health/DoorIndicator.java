package org.jibe77.hermanas.health;

import org.jibe77.hermanas.controller.door.DoorController;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DoorIndicator implements HealthIndicator {

    DoorController doorController;

    public DoorIndicator(DoorController doorController) {
        this.doorController = doorController;
    }

    @Override
    public Health health() {
        // this test is done only if the door is opened.
        if (doorController.doorIsOpened()) {
            doorController.closeDoorWithBottormButtonManagement(false);
            if (doorController.doorIsClosed()) {
                doorController.openDoor(false);
                return Health.up().build();
            }
            return Health.down().build();
        }
        return Health.up().build();
    }
}
