package org.jibe77.hermanas.scheduler.detail;

import org.jibe77.hermanas.scheduler.job.CameraJob;
import org.jibe77.hermanas.scheduler.job.SunRelatedJob;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class CameraJobDetail extends JobDetailFactoryBean {

    public CameraJobDetail() {
        setJobClass(CameraJob.class);
        setDescription("Starting job related to camera ...");
        setDurability(true);
    }
}
