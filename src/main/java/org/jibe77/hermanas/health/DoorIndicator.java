package org.jibe77.hermanas.health;

import org.jibe77.hermanas.controller.camera.CameraService;
import org.jibe77.hermanas.controller.door.DoorService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DoorIndicator implements HealthIndicator {

    DoorService doorService;
    CameraService cameraService;

    public DoorIndicator(DoorService doorService, CameraService cameraService) {
        this.doorService = doorService;
        this.cameraService = cameraService;
    }

    @Override
    public Health health() {
        // this test is done only if the door is opened.
        if (doorService.doorIsOpened()) {
            doorService.closeDoorWithBottormButtonManagement(false);
            if (doorService.doorIsClosed()) {
                doorService.openDoorWithUpButtonManagment(false, false);
                return Health.up().build();
            }
            return Health.down().build();
        }
        return Health.up().build();
    }
}
