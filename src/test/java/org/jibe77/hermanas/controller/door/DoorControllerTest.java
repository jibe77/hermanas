package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoorControllerTest {

    @Test
    void status() {
        DoorPictureAnalizer doorPictureAnalizer =
                mock(DoorPictureAnalizer.class);
        when(doorPictureAnalizer.isDoorClosed()).thenReturn(true);

        DoorController doorController = new DoorController(
                mock(ServoMotorController.class),
                doorPictureAnalizer,
                mock(CameraController.class)
                );
        assertEquals("UNDEFINED", doorController.status());

        doorController.closeDoorWithPictureAnalysis(true);
        doorController.closeDoorWithPictureAnalysis(true);
        assertEquals("CLOSED", doorController.status());

        doorController.openDoor(true, false);
        doorController.openDoor(true, false);
        assertEquals("OPENED", doorController.status());
    }
}