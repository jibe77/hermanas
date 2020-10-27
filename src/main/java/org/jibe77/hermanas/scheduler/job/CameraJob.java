package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CameraJob {

    private CameraController cameraController;

    private SunTimeUtils sunTimeUtils;

    @Value("${camera.scheduler.by.night}")
    private boolean takingPicturesByNight;

    Logger logger = LoggerFactory.getLogger(CameraJob.class);

    public CameraJob(CameraController cameraController, SunTimeUtils sunTimeUtils) {
        this.cameraController = cameraController;
        this.sunTimeUtils = sunTimeUtils;
    }

    @Scheduled(fixedDelayString = "${camera.scheduler.delay.in.milliseconds}")
    public void execute() {
        if (sunTimeUtils.isDay() || takingPicturesByNight) {
            try {
                logger.info("Camera scheduled job is taking a picture now.");
                cameraController.takePicture(true);
            } catch (IOException e) {
                logger.error("Can't take picture or write picture of filesystem.", e);
            }
        } else {
            logger.info("Camera scheduler is not taking pictures by night.");
        }
    }
}