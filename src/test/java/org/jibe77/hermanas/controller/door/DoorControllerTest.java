package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DoorControllerTest {

    @Test
    void status() {

        DoorController doorController = new DoorController(
                mock(ServoMotorController.class)
                );
        assertEquals("UNDEFINED", doorController.status());

        doorController.closeDoor(true);
        doorController.closeDoor(true);
        assertEquals("CLOSED", doorController.status());

        doorController.openDoor(true, false);
        doorController.openDoor(true, false);
        assertEquals("OPENED", doorController.status());
    }
}