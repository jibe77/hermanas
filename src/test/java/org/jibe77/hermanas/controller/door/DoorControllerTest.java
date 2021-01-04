package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DoorControllerTest {

    @Test
    void status() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer =
                mock(DoorPictureAnalizer.class);
        when(doorPictureAnalizer.isDoorClosed(null)).thenReturn(true);
        CameraController cameraController = mock(CameraController.class);
        BottomButtonController bottomButtonController = mock(BottomButtonController.class);
        UpButtonController upButtonController = mock(UpButtonController.class);
        when(cameraController.takePictureNoException(true)).thenReturn(Optional.of(new File("")));
        when(doorPictureAnalizer.getClosedStatus(any())).thenReturn(100);
        when(doorPictureAnalizer.isDoorClosed(any())).thenReturn(true);
        DoorController doorController = new DoorController(
                mock(ServoMotorController.class),
                bottomButtonController,
                upButtonController,
                mock(SunTimeManager.class)
                );
        assertEquals(DoorStatus.UNDEFINED, doorController.status());

        when(bottomButtonController.isBottomButtonPressed()).thenReturn(true);
        assertEquals(DoorStatus.CLOSED, doorController.status());
        when(upButtonController.isUpButtonPressed()).thenReturn(true);
        assertEquals(DoorStatus.OPENED, doorController.status());
    }
}
