package org.jibe77.hermanas.scheduler.trigger;

import org.jibe77.hermanas.scheduler.detail.CameraJobDetail;
import org.jibe77.hermanas.scheduler.detail.SunRelatedJobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CameraTrigger extends SimpleTriggerFactoryBean {

    @Value("${camera.scheduler.trigger.interval}")
    private int triggerInterval;

    CameraJobDetail cameraJobDetail;

    public CameraTrigger(CameraJobDetail cameraJobDetail) {
        this.cameraJobDetail = cameraJobDetail;
    }

    @PostConstruct
    private void init() {
        setJobDetail(cameraJobDetail.getObject());
        setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        setRepeatInterval(triggerInterval);
    }
}
