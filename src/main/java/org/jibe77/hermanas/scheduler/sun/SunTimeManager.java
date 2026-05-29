package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.scheduler.sun.model.NextEvents;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SunTimeManager {

    SunTimeUtils sunTimeUtils;
    private final ConfigService configService;

    public static final String HH_MM = "HH:mm";

    private static final Logger logger = LoggerFactory.getLogger(SunTimeManager.class);

    public SunTimeManager(SunTimeUtils sunTimeUtils, ConfigService configService) {
        this.sunTimeUtils = sunTimeUtils;
        this.configService = configService;
    }

    @Cacheable(value = "light-on")
    public LocalDateTime getNextLightOnTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(
                -1 * configService.getLightOnTimeBeforeSunset());
        logger.info("computing next light switching on time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-opening")
    public LocalDateTime getNextDoorOpeningTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunriseEvent(
                configService.getDoorOpenTimeAfterSunrise());
        logger.info("computing next door opening time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-closing")
    public LocalDateTime getNextDoorClosingTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(
                configService.getDoorCloseTimeAfterSunset());
        logger.info("computing next door closing time : {}", localDateTime);
        return localDateTime;
    }

    public NextEvents getNextEvents() {
        return new NextEvents(getNextDoorOpeningTime(), getNextLightOnTime(), getNextDoorClosingTime());
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

    public DoorStatusEnum getExpectedDoorStatus() {
        return getNextDoorOpeningTime().isBefore(getNextDoorClosingTime()) ? DoorStatusEnum.CLOSED : DoorStatusEnum.OPENED;
    }
}
