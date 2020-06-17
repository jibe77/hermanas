package org.jibe77.hermanas.scheduler;

import org.jibe77.hermanas.scheduler.util.SunTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Scope("singleton")
public class SunHourService {

    Logger logger = LoggerFactory.getLogger(SunHourService.class);

    @Cacheable("light-on")
    public LocalDateTime getNextLightOnTime() {
        LocalDateTime localDateTime = SunTimeUtils.computeNextSunset();
        logger.info("computing next light switching on time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("light-off")
    public LocalDateTime getNextLightOffTime() {
        LocalDateTime localDateTime = SunTimeUtils.computeNextSunset().plusMinutes(20);
        logger.info("computing next light switching off time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-opening")
    public LocalDateTime getNextDoorOpeningTime() {
        LocalDateTime localDateTime = SunTimeUtils.computeNextSunrise().plusMinutes(15);
        logger.info("computing next door opening time : {}", localDateTime.toString());
        return localDateTime;
    }

    @Cacheable("door-closing")
    public LocalDateTime getNextDoorClosingTime() {
        LocalDateTime localDateTime = SunTimeUtils.computeNextSunset().plusMinutes(15);
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
