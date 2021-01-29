package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.controller.door.DoorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunTimeManager {

    SunTimeUtils sunTimeUtils;

    @Value("${suntime.scheduler.light.on.time_before_sunset}")
    private long lightOnTimeBeforeSunset;

    @Value("${suntime.scheduler.door.close.time_after_sunset}")
    private long doorCloseTimeAfterSunset;

    @Value("${suntime.scheduler.door.open.time_after_sunrise}")
    private long doorOpenTimeAfterSunrise;

    public static final String HH_MM = "HH:mm";

    Logger logger = LoggerFactory.getLogger(SunTimeManager.class);

    public SunTimeManager(SunTimeUtils sunTimeUtils) {
        this.sunTimeUtils = sunTimeUtils;;
    }

    @Cacheable(value = "light-on")
    public LocalDateTime getNextLightOnTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(-1 * lightOnTimeBeforeSunset);
        logger.info("computing next light switching on time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-opening")
    public LocalDateTime getNextDoorOpeningTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunriseEvent(doorOpenTimeAfterSunrise);
        logger.info("computing next door opening time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-closing")
    public LocalDateTime getNextDoorClosingTime() {
        // in winter, the door is closed 10 minutes earlier.
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(doorCloseTimeAfterSunset);
        logger.info("computing next door closing time : {}", localDateTime);
        return localDateTime;
    }

    @CacheEvict(value = "door-closing")
    public void reloadDoorClosingTime() {
        logger.info("revoke cache on door closing time.");
    }

    @CacheEvict("door-opening")
    public void reloadDoorOpeningTime() {
        logger.info("revoke cache on door opening time.");
    }

    @CacheEvict("light-on")
    public void reloadLightOnTime() {
        logger.info("revoke cache on light switching on time.");
    }

    public DoorStatus getExpectedDoorStatus() {
        int currentDay = LocalDateTime.now().getDayOfMonth();
        if (currentDay != getNextDoorOpeningTime().getDayOfMonth() && currentDay == getNextDoorClosingTime().getDayOfMonth()) {
            return DoorStatus.OPENED;
        } else {
            return DoorStatus.CLOSED;
        }
    }
}
