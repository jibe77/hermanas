package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.SunRelatedJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SunRelatedTrigger extends SimpleTriggerFactoryBean {

    @Value("${suntime.scheduler.trigger.interval}")
    private int triggerInterval;

    SunRelatedJobDetail sunRelatedJobDetail;

    public SunRelatedTrigger(SunRelatedJobDetail sunRelatedJobDetail) {
        this.sunRelatedJobDetail = sunRelatedJobDetail;
    }

    @PostConstruct
    private void init() {
        setJobDetail(sunRelatedJobDetail.getObject());
        setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        setRepeatInterval(triggerInterval);
    }
}
