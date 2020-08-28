package org.jibe77.hermanas.health;

import org.jibe77.hermanas.client.jms.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class JmsAndCameraHealthIndicator implements HealthIndicator {

    CameraController cameraController;
    EmailService emailService;

    public JmsAndCameraHealthIndicator(CameraController cameraController, EmailService emailService) {
        this.cameraController = cameraController;
        this.emailService = emailService;
    }

    @Override
    public Health health() {
        Optional<File> picWithClosedDoor = cameraController.takePictureNoException();
        if (picWithClosedDoor.isPresent()) {
            emailService.sendPicture(
                    "Notification sent by Actuator on Hermanas",
                    picWithClosedDoor);
            return Health.up().build();
        } else {
            return Health.down().withDetail("picture","not present").build();
        }
    }
}
