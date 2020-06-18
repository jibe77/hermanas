package org.jibe77.hermanas.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Scope("singleton")
public class SunTimeService {

    Logger logger = LoggerFactory.getLogger(SunTimeService.class);

    SunTimeUtils sunTimeUtils;

    public SunTimeService(SunTimeUtils sunTimeUtils) {
        this.sunTimeUtils = sunTimeUtils;
    }

    @Cacheable("light-on")
    public LocalDateTime getNextLightOnTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset();
        logger.info("computing next light switching on time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("light-off")
    public LocalDateTime getNextLightOffTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset().plusMinutes(20);
        logger.info("computing next light switching off time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-opening")
    public LocalDateTime getNextDoorOpeningTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunrise().plusMinutes(15);
        logger.info("computing next door opening time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-closing")
    public LocalDateTime getNextDoorClosingTime() {
        LocalDateTime localDateTime = sunTimeUtils.computeNextSunset().plusMinutes(15);
        logger.info("computing next door closing time : {}", localDateTime.toString());
        return localDateTime;
    }
    @CacheEvict(value= "door-closing")
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
