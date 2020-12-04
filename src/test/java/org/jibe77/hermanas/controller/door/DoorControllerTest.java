package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoorControllerTest {

    @Test
    void status() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer =
                mock(DoorPictureAnalizer.class);
        when(doorPictureAnalizer.isDoorClosed()).thenReturn(true);
        CameraController cameraController = mock(CameraController.class);
        when(cameraController.takePictureNoException(true)).thenReturn(Optional.of(new File("")));
        when(doorPictureAnalizer.getClosedStatus(any())).thenReturn(100);
        when(doorPictureAnalizer.isDoorClosed(any())).thenReturn(true);
        DoorController doorController = new DoorController(
                mock(ServoMotorController.class),
                doorPictureAnalizer,
                cameraController
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