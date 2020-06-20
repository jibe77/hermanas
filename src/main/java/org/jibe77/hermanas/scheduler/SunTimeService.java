package org.jibe77.hermanas.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Scope("singleton")
public class SunTimeService {

    SunTimeUtils sunTimeUtils;

    @Value("${suntime.scheduler.light.on.time_before_sunset}")
    private int lightOnTimeBeforeSunset;

    @Value("${suntime.scheduler.light.off.time_after_sunset}")
    private int lightOffTimeAfterSunset;

    @Value("${suntime.scheduler.door.close.time_after_sunset}")
    private int doorCloseTimeAfterSunset;

    @Value("${suntime.scheduler.door.open.time_after_sunrise}")
    private int doorOpenTimeAfterSunrise;

    Logger logger = LoggerFactory.getLogger(SunTimeService.class);

    public SunTimeService(SunTimeUtils sunTimeUtils) {
        this.sunTimeUtils = sunTimeUtils;
    }

    @Cacheable("light-on")
    public LocalDateTime getNextLightOnTime(LocalDateTime currentTime) {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset(
                currentTime.minusMinutes(lightOnTimeBeforeSunset)
                    ).plusMinutes(lightOnTimeBeforeSunset);
        logger.info("computing next light switching on time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("light-off")
    public LocalDateTime getNextLightOffTime(LocalDateTime currentTime) {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset(
                currentTime.minusMinutes(lightOffTimeAfterSunset)
                    ).plusMinutes(lightOffTimeAfterSunset);
        logger.info("computing next light switching off time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-opening")
    public LocalDateTime getNextDoorOpeningTime(LocalDateTime currentTime) {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunrise(
                currentTime.minusMinutes(doorOpenTimeAfterSunrise)
                    ).plusMinutes(doorOpenTimeAfterSunrise);
        logger.info("computing next door opening time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-closing")
    public LocalDateTime getNextDoorClosingTime(LocalDateTime currentTime) {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset(currentTime.minusMinutes(doorCloseTimeAfterSunset)).plusMinutes(doorCloseTimeAfterSunset);
        logger.info("computing next door closing time : {}", localDateTime.toString());
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

    @CacheEvict("light-off")
    public void reloadLightOffTime() {
        logger.info("revoke cache on light switching off time.");
    }

}
