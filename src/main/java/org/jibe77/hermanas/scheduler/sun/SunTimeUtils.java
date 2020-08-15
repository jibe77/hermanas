package org.jibe77.hermanas.scheduler.sun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

@Component
@Scope("singleton")
public class SunTimeUtils {

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    Logger logger = LoggerFactory.getLogger(SunTimeUtils.class);

    @PostConstruct
    private void init() {
        logger.info("Sun time utils configured with latitude {} and longitude {}.", latitude, longitude);
    }

    /**
     * Compute LocalDateTime for an event occuring in X minutes after the sunset
     * @param minutes minutes after the sunset, it is possible to specify
     *                a negative value for an event before the sunset
     * @return Time of the event after the sunset
     */
    protected LocalDateTime computeTimeForNextSunsetEvent(long minutes) {
        return computeTimeForNextSunsetEvent(LocalDateTime.now(), minutes);
    }

    protected LocalDateTime computeTimeForNextSunriseEvent(long minutes) {
        return computeTimeForNextSunriseEvent(LocalDateTime.now(), minutes);
    }

    protected LocalDateTime computeTimeForNextSunsetEvent(LocalDateTime date, long minutes) {
        return computeTimeForNextSunsetEvent(date.minusMinutes(minutes)).plusMinutes(minutes);
    }

    protected ZonedDateTime computeTimeForNextSunsetEvent(ZonedDateTime dateTime, long minutes) {
        LocalDateTime localDateTime = computeTimeForNextSunsetEvent(convertZonedToLocalDateTime(dateTime), minutes);
        return convertLocalToZonedDateTime(localDateTime, dateTime.getZone());
    }

    protected LocalDateTime computeTimeForNextSunriseEvent(LocalDateTime date, long minutes) {
        return computeTimeForNextSunriseEvent(date.minusMinutes(minutes)).plusMinutes(minutes);
    }

    protected LocalDateTime computeTimeForNextSunsetEvent(LocalDateTime date) {
        LocalDateTime currentDaySunset = computeCurrentDaySunset(date);
        if(date.isAfter(currentDaySunset)) {
            return computeNextDaySunset(date);
        } else {
            return currentDaySunset;
        }
    }

    protected LocalDateTime computeTimeForNextSunriseEvent(LocalDateTime date) {
        LocalDateTime currentDaySunrise = computeCurrentDaySunrise(date);
        if(date.isAfter(currentDaySunrise)) {
            return computeNextDaySunrise(date);
        } else {
            return currentDaySunrise;
        }
    }

    private LocalDateTime computeNextDaySunset(LocalDateTime date) {
        return computeCurrentDaySunset(date.plusDays(1));
    }

    private LocalDateTime computeNextDaySunrise(LocalDateTime date) {
        return computeCurrentDaySunrise(date.plusDays(1));
    }

    private LocalDateTime computeCurrentDaySunrise(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[0]);
    }

    private LocalDateTime computeCurrentDaySunset(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[1]);
    }
    private Calendar[] computeCurrentDay(LocalDateTime date) {
        return ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(
                localDateTimeToCalendar(date),
                latitude,
                longitude);
    }

    private Calendar localDateTimeToCalendar(LocalDateTime localDateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(localDateTime.getYear(), localDateTime.getMonthValue()-1, localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        return calendar;
    }

    private LocalDateTime calendarToLocalDateTime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        TimeZone tz = calendar.getTimeZone();
        ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
        return LocalDateTime.ofInstant(calendar.toInstant(), zid);
    }

    private ZonedDateTime convertLocalToZonedDateTime(LocalDateTime localDateTime, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        return zonedDateTime.withZoneSameInstant(zoneId);
    }

    private LocalDateTime convertZonedToLocalDateTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
