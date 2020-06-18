package org.jibe77.hermanas.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

@Component
@Scope("singleton")
public class SunTimeUtils {

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    protected LocalDateTime computeNextSunset() {
        return computeNextSunset(LocalDateTime.now());
    }

    protected LocalDateTime computeNextSunrise() {
        return computeNextSunrise(LocalDateTime.now());
    }

    protected LocalDateTime computeNextSunset(LocalDateTime date) {
        LocalDateTime currentDaySunset = computeCurrentDaySunset(date);
        if(date.isAfter(currentDaySunset)) {
            return computeNextDaySunset(date);
        } else {
            return currentDaySunset;
        }
    }

    protected LocalDateTime computeNextSunrise(LocalDateTime date) {
        LocalDateTime currentDaySunrise = computeCurrentDaySunrise(date);
        if(date.isAfter(currentDaySunrise)) {
            return computeNextDaySunrise(date);
        } else {
            return currentDaySunrise;
        }
    }


    protected LocalDateTime computeNextDaySunset(LocalDateTime date) {
        return computeCurrentDaySunset(date.plusDays(1));
    }


    protected LocalDateTime computeNextDaySunrise(LocalDateTime date) {
        return computeCurrentDaySunrise(date.plusDays(1));
    }

    protected LocalDateTime computeCurrentDaySunrise(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[0]);
    }

    protected LocalDateTime computeCurrentDaySunset(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[1]);
    }
    protected Calendar[] computeCurrentDay(LocalDateTime date) {
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
}
