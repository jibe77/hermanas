package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraService;
import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonService;
import org.jibe77.hermanas.controller.door.model.DoorStatusEnum;
import org.jibe77.hermanas.controller.door.servo.ServoMotorService;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonService;
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
        CameraService cameraService = mock(CameraService.class);
        BottomButtonService bottomButtonService = mock(BottomButtonService.class);
        UpButtonService upButtonService = mock(UpButtonService.class);
        when(cameraService.takePictureNoException(true)).thenReturn(Optional.of(new File("")));
        when(doorPictureAnalizer.getClosedStatus(any())).thenReturn(100);
        when(doorPictureAnalizer.isDoorClosed(any())).thenReturn(true);
        DoorService doorService = new DoorService(
                mock(ServoMotorService.class),
                bottomButtonService,
                upButtonService,
                mock(SunTimeManager.class),
                mock(NotificationController.class)
                );
        assertEquals(DoorStatusEnum.UNDEFINED, doorService.statusInfo().getStatus());

        when(bottomButtonService.isBottomButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.CLOSED, doorService.statusInfo().getStatus());
        when(upButtonService.isUpButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.OPENED, doorService.statusInfo().getStatus());
    }
}
