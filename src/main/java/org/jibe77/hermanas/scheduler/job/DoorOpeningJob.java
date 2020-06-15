package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.scheduler.trigger.SunsetTrigger;
import org.jibe77.hermanas.scheduler.util.SunTimeUtils;
import org.jibe77.hermanas.service.DoorService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DoorOpeningJob implements Job {

    @Autowired
    /**
     * Quartz is instantiating the job with default constructor,
     * so it's not possible to inject beans with constructor.
     */
    public DoorService doorService;

    @Autowired
    SunsetTrigger sunsetTrigger;

    Logger logger = LoggerFactory.getLogger(DoorOpeningJob.class);

    public void execute(JobExecutionContext context) {
        logger.info("start door opening at sunrise.");
        doorService.open();
        sunsetTrigger.setStartTime(SunTimeUtils.computeNextSunriseAsDate());
    }
}