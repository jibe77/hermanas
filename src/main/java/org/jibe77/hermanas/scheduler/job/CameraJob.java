package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.camera.CameraController;
import org.jibe77.hermanas.service.DoorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CameraJob {

    private CameraController cameraController;

    Logger logger = LoggerFactory.getLogger(CameraJob.class);

    public CameraJob(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    @Scheduled(fixedDelayString = "${camera.scheduler.delay.in.milliseconds}")
    public void execute() {
        try {
            logger.info("Camera scheduled job is taking a picture now.");
            cameraController.takePicture();
        } catch (IOException e) {
            logger.error("Can't take picture or write picture of filesystem.", e);
        }
    }
}