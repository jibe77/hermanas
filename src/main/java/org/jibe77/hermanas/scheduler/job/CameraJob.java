package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CameraJob {

    private CameraService cameraService;

    private SunTimeUtils sunTimeUtils;

    @Value("${camera.scheduler.by.night}")
    private boolean takingPicturesByNight;

    private static final Logger logger = LoggerFactory.getLogger(CameraJob.class);

    public CameraJob(CameraService cameraService, SunTimeUtils sunTimeUtils) {
        this.cameraService = cameraService;
        this.sunTimeUtils = sunTimeUtils;
    }

    @Scheduled(fixedDelayString = "${camera.scheduler.delay.in.milliseconds}")
    public void execute() {
        if (sunTimeUtils.isDay() || takingPicturesByNight) {
            try {
                logger.info("Camera scheduled job is taking a picture now.");
                cameraService.takePicture(true);
            } catch (IOException | InterruptedException e) {
                logger.error("Can't take picture or write picture of filesystem.", e);
            }
        } else {
            logger.info("Camera scheduler is not taking pictures by night.");
        }
    }
}
