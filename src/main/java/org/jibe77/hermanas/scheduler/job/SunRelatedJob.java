package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.scheduler.event.ManageDoorClosingEvent;
import org.jibe77.hermanas.scheduler.event.ManageDoorOpeningEvent;
import org.jibe77.hermanas.scheduler.event.ManageLightSwitchingOnEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunRelatedJob {

    ManageDoorClosingEvent manageDoorClosingEvent;

    ManageDoorOpeningEvent manageDoorOpeningEvent;

    ManageLightSwitchingOnEvent manageLightSwitchingOnEvent;

    public SunRelatedJob(ManageDoorClosingEvent manageDoorClosingEvent, ManageDoorOpeningEvent manageDoorOpeningEvent, ManageLightSwitchingOnEvent manageLightSwitchingOnEvent) {
        this.manageDoorClosingEvent = manageDoorClosingEvent;
        this.manageDoorOpeningEvent = manageDoorOpeningEvent;
        this.manageLightSwitchingOnEvent = manageLightSwitchingOnEvent;
    }

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    void execute() {
        LocalDateTime currentTime = LocalDateTime.now();
        manageDoorClosingEvent.manageDoorClosingEvent(currentTime);
        manageDoorOpeningEvent.manageDoorOpeningEvent(currentTime);
        manageLightSwitchingOnEvent.manageLightSwitchingOnEvent(currentTime);
    }
}