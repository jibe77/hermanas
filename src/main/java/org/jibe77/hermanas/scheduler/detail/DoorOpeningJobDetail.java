package org.jibe77.hermanas.scheduler.detail;
import org.jibe77.hermanas.scheduler.job.DoorOpeningJob;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DoorOpeningJobDetail extends JobDetailFactoryBean {

    public DoorOpeningJobDetail() {
        setJobClass(DoorOpeningJob.class);
        setDescription("Initialising door opening Job ...");
        setDurability(true);
    }
}
