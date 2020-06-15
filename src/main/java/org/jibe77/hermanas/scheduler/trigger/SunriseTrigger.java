package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.DoorOpeningJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class SunriseTrigger extends SimpleTriggerFactoryBean {

    public SunriseTrigger(DoorOpeningJobDetail doorOpeningJobDetail) {
        setJobDetail(doorOpeningJobDetail.getObject());
        setRepeatInterval(10000);
        //trigger.setStartTime(new Date());
        setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    }
}
