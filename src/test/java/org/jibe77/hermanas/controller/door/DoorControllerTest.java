package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoorControllerTest {

    @Test
    void status() {
        BottomButtonController bottomButtonController =
                mock(BottomButtonController.class);
        when(bottomButtonController.isBottomButtonHasBeenPressed()).thenReturn(true);

        DoorController doorController = new DoorController(
                mock(ServoMotorController.class),
                bottomButtonController
                );
        assertEquals("UNDEFINED", doorController.status());

        doorController.closeDoorWithBottormButtonManagement(true);
        doorController.closeDoorWithBottormButtonManagement(true);
        assertEquals("CLOSED", doorController.status());

        doorController.openDoor(true);
        doorController.openDoor(true);
        assertEquals("OPENED", doorController.status());
    }
}