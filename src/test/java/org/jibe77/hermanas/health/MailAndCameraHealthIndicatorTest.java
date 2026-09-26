package org.jibe77.hermanas.health;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.service.camera.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MailAndCameraHealthIndicatorTest {

    MailAndCameraHealthIndicator healthIndicator;

    CameraService cameraService = mock(CameraService.class);

    EmailService emailService = mock(EmailService.class);

    @BeforeEach
    void setUp() {
        healthIndicator = new MailAndCameraHealthIndicator(cameraService, emailService);
    }

    @Test
    void returnsUpAndSendsMailToAdminsWhenPictureIsPresent() {
        Optional<File> picture = Optional.of(new File("test-picture.jpg"));
        when(cameraService.takePictureNoException(false)).thenReturn(picture);

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        verify(emailService, times(1))
                .sendMailToAdmins(anyString(), anyString(), any(Optional.class));
    }

    @Test
    void returnsDownAndDoesNotSendMailWhenPictureIsAbsent() {
        when(cameraService.takePictureNoException(false)).thenReturn(Optional.empty());

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("not present", health.getDetails().get("picture"));
        verify(emailService, never())
                .sendMailToAdmins(anyString(), anyString(), any(Optional.class));
    }

    @Test
    void doesNotCallSendMailButSendMailToAdmins() {
        Optional<File> picture = Optional.of(new File("test-picture.jpg"));
        when(cameraService.takePictureNoException(false)).thenReturn(picture);

        healthIndicator.health();

        verify(emailService, never()).sendMail(anyString(), anyString(), any(Optional.class));
        verify(emailService, times(1)).sendMailToAdmins(anyString(), anyString(), any(Optional.class));
    }
}
