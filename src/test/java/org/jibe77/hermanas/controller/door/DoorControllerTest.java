package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.model.DoorStatusEnum;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.websocket.NotificationController;
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
                mock(SunTimeManager.class),
                mock(NotificationController.class)
                );
        assertEquals(DoorStatusEnum.UNDEFINED, doorController.statusInfo().getStatus());

        when(bottomButtonController.isBottomButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.CLOSED, doorController.statusInfo().getStatus());
        when(upButtonController.isUpButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.OPENED, doorController.statusInfo().getStatus());
    }
}
