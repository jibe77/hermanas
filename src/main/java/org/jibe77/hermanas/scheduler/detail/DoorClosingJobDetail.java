package org.jibe77.hermanas.scheduler.detail;
import org.jibe77.hermanas.scheduler.job.DoorClosingJob;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DoorClosingJobDetail extends JobDetailFactoryBean {

    public DoorClosingJobDetail() {
        setJobClass(DoorClosingJob.class);
        setDescription("Initialising door closing Job ...");
        setDurability(true);
    }
}
