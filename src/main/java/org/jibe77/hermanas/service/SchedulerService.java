package org.jibe77.hermanas.service;

import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.scheduler.sun.model.NextEvents;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@CrossOrigin
@RestController
public class SchedulerService {

    SunTimeManager sunTimeManager;

    public SchedulerService(SunTimeManager sunTimeManager) {
        this.sunTimeManager = sunTimeManager;
    }

    @CrossOrigin
    @GetMapping(value = "/scheduler/doorClosingTime")
    public String getNextDoorClosingTime() {
        return sunTimeManager.getNextDoorClosingTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    @CrossOrigin
    @GetMapping(value = "/scheduler/doorOpeningTime")
    public String getNextDoorOpeningTime() {
        return sunTimeManager.getNextDoorOpeningTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    @GetMapping(value = "/scheduler/lightOnTime")
    public String getNextLightOnTime() {
        return sunTimeManager.getNextLightOnTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    @CrossOrigin
    @GetMapping(value = "/scheduler/nextEvents")
    public NextEvents getNextEvents() {
        return sunTimeManager.getNextEvents();
    }

}
