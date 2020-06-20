package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.camera.CameraController;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.scheduler.SunTimeService;
import org.jibe77.hermanas.service.DoorService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CameraJob implements Job {

    @Autowired
    CameraController cameraController;

    @Autowired
    /**
     * Quartz is instantiating the job with default constructor,
     * so it's not possible to inject beans with constructor.
     */
    public DoorService doorService;

    Logger logger = LoggerFactory.getLogger(CameraJob.class);

    public void execute(JobExecutionContext context) {
        cameraController.takePicture();
    }
}