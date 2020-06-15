package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.DoorClosingJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class SunsetTrigger extends SimpleTriggerFactoryBean {

    public SunsetTrigger(DoorClosingJobDetail doorClosingJobDetail) {
        setJobDetail(doorClosingJobDetail.getObject());
        Calendar[] cals = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), 48.85837, 2.294481);
        setStartTime(cals[0].getTime());
    }
}
