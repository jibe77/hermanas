package org.jibe77.hermanas.scheduler.detail;

import org.jibe77.hermanas.scheduler.job.SunRelatedJob;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class SunRelatedJobDetail extends JobDetailFactoryBean {

    public SunRelatedJobDetail() {
        setJobClass(SunRelatedJob.class);
        setDescription("Starting job related to sun hours ...");
        setDurability(true);
    }
}
