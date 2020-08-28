package org.jibe77.hermanas.service;

import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class SchedulerService {

    public static final String HH_MM = "HH:mm";
    SunTimeManager sunTimeManager;

    public SchedulerService(SunTimeManager sunTimeManager) {
        this.sunTimeManager = sunTimeManager;
    }

    @GetMapping(value = "/scheduler/doorClosingTime")
    public String getNextDoorClosingTime() {
        LocalDateTime localDateTime = sunTimeManager.getNextDoorClosingTime();
        // add 1 minute because the cron task is started every minutes, so the event is not starting at this very moment.
        return localDateTime.plusMinutes(1).format(DateTimeFormatter.ofPattern(HH_MM));
    }

    @GetMapping(value = "/scheduler/doorOpeningTime")
    public String getNextDoorOpeningTime() {
        LocalDateTime localDateTime = sunTimeManager.getNextDoorOpeningTime();
        // add 1 minute because the cron task is started every minutes, so the event is not starting at this very moment.
        return localDateTime.plusMinutes(1).format(DateTimeFormatter.ofPattern(HH_MM));
    }

    @GetMapping(value = "/scheduler/lightOffTime")
    public String getNextLightOffTime() {
        LocalDateTime localDateTime = sunTimeManager.getNextLightOffTime();
        // add 1 minute because the cron task is started every minutes, so the event is not starting at this very moment.
        return localDateTime.plusMinutes(1).format(DateTimeFormatter.ofPattern(HH_MM));
    }

    @GetMapping(value = "/scheduler/lightOnTime")
    public String getNextLightOnTime() {
        LocalDateTime localDateTime = sunTimeManager.getNextLightOnTime();
        // add 1 minute because the cron task is started every minutes, so the event is not starting at this very moment.
        return localDateTime.plusMinutes(1).format(DateTimeFormatter.ofPattern(HH_MM));
    }

}
