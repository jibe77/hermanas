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
        // Light-on rides on the sunset schedule. When the closing is forced,
        // the "sunset" is really the fixed closing time, so the light-on offset
        // still needs to run *before* it — we keep the negative offset here.
        long offset = -1 * configService.getLightOnTimeBeforeSunset();
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(offset);
        logger.info("computing next light switching on time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-opening")
    public LocalDateTime getNextDoorOpeningTime() {
        // In force mode the operator has set an exact HH:mm — honouring the
        // "delay after sunrise" on top of that would silently drift the door
        // to a time that no longer matches the label shown in the UI.
        long offset = configService.isDoorOpeningForceEnabled()
                ? 0L
                : configService.getDoorOpenTimeAfterSunrise();
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunriseEvent(offset);
        logger.info("computing next door opening time : {}", localDateTime);
        return localDateTime;
    }

    @Cacheable(value = "door-closing")
    public LocalDateTime getNextDoorClosingTime() {
        long offset = configService.isDoorClosingForceEnabled()
                ? 0L
                : configService.getDoorCloseTimeAfterSunset();
        LocalDateTime localDateTime = sunTimeUtils.computeTimeForNextSunsetEvent(offset);
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
