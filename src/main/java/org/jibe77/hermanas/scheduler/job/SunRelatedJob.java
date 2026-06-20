package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.scheduler.event.ManageDoorClosingEvent;
import org.jibe77.hermanas.scheduler.event.ManageDoorOpeningEvent;
import org.jibe77.hermanas.scheduler.event.ManageDoorVerificationEvent;
import org.jibe77.hermanas.scheduler.event.ManageLightSwitchingOnEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunRelatedJob {

    ManageDoorClosingEvent manageDoorClosingEvent;

    ManageDoorOpeningEvent manageDoorOpeningEvent;

    ManageLightSwitchingOnEvent manageLightSwitchingOnEvent;

    ManageDoorVerificationEvent manageDoorVerificationEvent;

    public SunRelatedJob(ManageDoorClosingEvent manageDoorClosingEvent,
                         ManageDoorOpeningEvent manageDoorOpeningEvent,
                         ManageLightSwitchingOnEvent manageLightSwitchingOnEvent,
                         ManageDoorVerificationEvent manageDoorVerificationEvent) {
        this.manageDoorClosingEvent = manageDoorClosingEvent;
        this.manageDoorOpeningEvent = manageDoorOpeningEvent;
        this.manageLightSwitchingOnEvent = manageLightSwitchingOnEvent;
        this.manageDoorVerificationEvent = manageDoorVerificationEvent;
    }

    @Scheduled(fixedDelayString = "${suntime.scheduler.delay.in.milliseconds}")
    void execute() {
        LocalDateTime currentTime = LocalDateTime.now();
        manageDoorClosingEvent.manageDoorClosingEvent(currentTime);
        manageDoorOpeningEvent.manageDoorOpeningEvent(currentTime);
        manageLightSwitchingOnEvent.manageLightSwitchingOnEvent(currentTime);
        // Verification fires 30 min after each scheduled open/close — has to come
        // after the open/close events above so a freshly-triggered movement has
        // had time to settle before we read the switches.
        manageDoorVerificationEvent.manageDoorVerificationEvent(currentTime);
    }
}