package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.SunRelatedJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class SunRelatedTrigger extends SimpleTriggerFactoryBean {

    public SunRelatedTrigger(SunRelatedJobDetail sunRelatedJobDetail) {
        setJobDetail(sunRelatedJobDetail.getObject());
        setRepeatInterval(300000); // every 5 minutes
        setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    }
}
