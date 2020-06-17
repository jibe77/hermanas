package org.jibe77.hermanas.scheduler;

import org.jibe77.hermanas.scheduler.detail.SunRelatedJobDetail;
import org.jibe77.hermanas.scheduler.trigger.SunRelatedTrigger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class SchedulerConfig {

    private final ApplicationContext applicationContext;

    public SchedulerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public SchedulerFactoryBean scheduler(
            SunRelatedTrigger sunRelatedTrigger,
            SunRelatedJobDetail sunRelatedJobDetail) {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setJobDetails(sunRelatedJobDetail.getObject());
        schedulerFactory.setTriggers(sunRelatedTrigger.getObject());
        return schedulerFactory;
    }

    public SpringBeanJobFactory springBeanJobFactory() {
        AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }
}
