package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.SunRelatedJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class SunRelatedTrigger extends SimpleTriggerFactoryBean {

    @Value("${suntime.scheduler.trigger.interval}")
    private int triggerInterval;

    public SunRelatedTrigger(SunRelatedJobDetail sunRelatedJobDetail) {
        setJobDetail(sunRelatedJobDetail.getObject());
        setRepeatInterval(triggerInterval);
        setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    }
}
