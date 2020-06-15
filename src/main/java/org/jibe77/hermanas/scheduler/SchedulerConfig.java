package org.jibe77.hermanas.scheduler;

import org.jibe77.hermanas.scheduler.detail.DoorClosingJobDetail;
import org.jibe77.hermanas.scheduler.detail.DoorOpeningJobDetail;
import org.jibe77.hermanas.scheduler.trigger.SunriseTrigger;
import org.jibe77.hermanas.scheduler.trigger.SunsetTrigger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class SchedulerConfig {

    private final ApplicationContext applicationContext;

    public SchedulerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public SchedulerFactoryBean scheduler(
            SunsetTrigger sunsetTrigger,
            SunriseTrigger sunriseTrigger,
            DoorClosingJobDetail doorClosingJobDetail,
            DoorOpeningJobDetail doorOpeningJobDetail) {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setJobDetails(doorClosingJobDetail.getObject(), doorOpeningJobDetail.getObject());
        schedulerFactory.setTriggers(sunriseTrigger.getObject(), sunsetTrigger.getObject());
        return schedulerFactory;
    }

    public SpringBeanJobFactory springBeanJobFactory() {
        AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }
}
