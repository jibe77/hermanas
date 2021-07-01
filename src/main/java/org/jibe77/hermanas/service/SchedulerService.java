package org.jibe77.hermanas.service;

import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
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

    public SchedulerService(SunTimeManager sunTimeManager,
                            ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
    }

    @GetMapping(value = "/scheduler/doorClosingTime")
    public String getNextDoorClosingTime() {
        return sunTimeManager.getNextDoorClosingTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    @GetMapping(value = "/scheduler/doorOpeningTime")
    public String getNextDoorOpeningTime() {
        return sunTimeManager.getNextDoorOpeningTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    @GetMapping(value = "/scheduler/lightOnTime")
    public String getNextLightOnTime() {
        return sunTimeManager.getNextLightOnTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM));
    }

    /**
     * result example :
     *
     * {"nextDoorOpeningTime":"2021-01-31T08:14:47","nextLightOnTime":"2021-01-30T17:28:49","nextDoorClosingTime":"2021-01-30T17:58:49"}
     * @return
     */
    @GetMapping(value = "/scheduler/nextEvents")
    public NextEvents getNextEvents() {
        return sunTimeManager.getNextEvents();
    }
}
