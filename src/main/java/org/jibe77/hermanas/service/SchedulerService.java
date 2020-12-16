package org.jibe77.hermanas.service;

import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return sunTimeManager.getNextDoorClosingTime().format(DateTimeFormatter.ofPattern(HH_MM));
    }

    @GetMapping(value = "/scheduler/doorOpeningTime")
    public String getNextDoorOpeningTime() {
        return sunTimeManager.getNextDoorOpeningTime().format(DateTimeFormatter.ofPattern(HH_MM));
    }

    @GetMapping(value = "/scheduler/lightOnTime")
    public String getNextLightOnTime() {
        return sunTimeManager.getNextLightOnTime().format(DateTimeFormatter.ofPattern(HH_MM));
    }

}
