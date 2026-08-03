package org.jibe77.hermanas.service.door;

import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.door.bottombutton.BottomButtonService;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.jibe77.hermanas.service.door.servo.ServoMotorService;
import org.jibe77.hermanas.service.door.upbutton.UpButtonService;
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
                mock(NotificationController.class),
                mock(org.jibe77.hermanas.service.config.ConfigService.class)
                );
        assertEquals(DoorStatusEnum.UNDEFINED, doorService.statusInfo().getStatus());

        when(bottomButtonService.isBottomButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.CLOSED, doorService.statusInfo().getStatus());
        when(upButtonService.isUpButtonPressed()).thenReturn(true);
        assertEquals(DoorStatusEnum.OPENED, doorService.statusInfo().getStatus());
    }

    /**
     * Le fin de course bas est usé : il rebondit et n'atteint pas toujours un niveau
     * haut franc. Une fermeture doit donc être validée par une simple agitation de la
     * ligne, faute d'appui net — sinon la porte est rouverte par sécurité alors
     * qu'elle est bien en bas.
     *
     * <p>Contournement d'un défaut matériel (migration.md §5.4). Ce test échouera si
     * quelqu'un rétablit le critère strict sans avoir remplacé le contact.</p>
     */
    @Test
    void closingIsValidatedByAnyBottomButtonActivity() {
        BottomButtonService bottomButtonService = mock(BottomButtonService.class);
        DoorService doorService = new DoorService(
                mock(ServoMotorService.class),
                bottomButtonService,
                mock(UpButtonService.class),
                mock(SunTimeManager.class),
                mock(NotificationController.class),
                mock(org.jibe77.hermanas.service.config.ConfigService.class));

        // Aucun appui franc, mais la ligne a bougé pendant la fermeture : la porte
        // est considérée comme fermée, sans réouverture de sécurité.
        when(bottomButtonService.isBottomButtonHasBeenPressed()).thenReturn(false);
        when(bottomButtonService.hasBottomButtonChanged()).thenReturn(true);
        when(bottomButtonService.isBottomButtonPressed()).thenReturn(false);

        assertDoesNotThrow(() -> doorService.closeDoorWithBottormButtonManagement(true));
        // Le mouvement de fermeture a bien été commandé, et aucune réouverture
        // de secours n'a suivi.
        verify(bottomButtonService).resetBottomButtonState();
    }

    /**
     * Symétrique du test précédent : sans le moindre signal du fin de course bas,
     * la fermeture reste un échec et la sécurité doit s'enclencher. La tolérance ne
     * doit pas devenir un blanc-seing.
     */
    @Test
    void closingStillFailsWhenBottomButtonIsCompletelySilent() {
        BottomButtonService bottomButtonService = mock(BottomButtonService.class);
        UpButtonService upButtonService = mock(UpButtonService.class);
        DoorService doorService = new DoorService(
                mock(ServoMotorService.class),
                bottomButtonService,
                upButtonService,
                mock(SunTimeManager.class),
                mock(NotificationController.class),
                mock(org.jibe77.hermanas.service.config.ConfigService.class));

        when(bottomButtonService.isBottomButtonHasBeenPressed()).thenReturn(false);
        when(bottomButtonService.hasBottomButtonChanged()).thenReturn(false);
        when(bottomButtonService.isBottomButtonPressed()).thenReturn(false);

        assertThrows(DoorNotClosedCorrectlyException.class,
                () -> doorService.closeDoorWithBottormButtonManagement(true));
    }
}
