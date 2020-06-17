package org.jibe77.hermanas.scheduler.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Update regulary timezone on JRE using command because daylight saving time will be soon removed :
 *  sudo java -jar tzupdater.jar -l https://data.iana.org/time-zones/tzdata-latest.tar.gz
 */
public class SunTimeUtils {

    public final static double LATITUDE = 49.37103491327111;
    public final static double LONGITUDE = 6.139976893987993;

    public static LocalDateTime computeNextSunset() {
        return computeNextSunset(LocalDateTime.now());
    }

    public static LocalDateTime computeNextSunrise() {
        return computeNextSunrise(LocalDateTime.now());
    }

    protected static LocalDateTime computeNextSunset(LocalDateTime date) {
        LocalDateTime currentDaySunset = computeCurrentDaySunset(date);
        if(date.isAfter(currentDaySunset)) {
            return computeNextDaySunset(date);
        } else {
            return currentDaySunset;
        }
    }

    protected static LocalDateTime computeNextSunrise(LocalDateTime date) {
        LocalDateTime currentDaySunrise = computeCurrentDaySunrise(date);
        if(date.isAfter(currentDaySunrise)) {
            return computeNextDaySunrise(date);
        } else {
            return currentDaySunrise;
        }
    }


    protected static LocalDateTime computeNextDaySunset(LocalDateTime date) {
        return computeCurrentDaySunset(date.plusDays(1));
    }


    protected static LocalDateTime computeNextDaySunrise(LocalDateTime date) {
        return computeCurrentDaySunrise(date.plusDays(1));
    }

    protected static LocalDateTime computeCurrentDaySunrise(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[0]);
    }

    protected static LocalDateTime computeCurrentDaySunset(LocalDateTime date) {
        return calendarToLocalDateTime(computeCurrentDay(date)[1]);
    }
    protected static Calendar[] computeCurrentDay(LocalDateTime date) {
        return ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(
                localDateTimeToCalendar(date),
                SunTimeUtils.LATITUDE,
                SunTimeUtils.LONGITUDE);
    }

    public static Calendar localDateTimeToCalendar(LocalDateTime localDateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(localDateTime.getYear(), localDateTime.getMonthValue()-1, localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        return calendar;
    }

    private static LocalDateTime calendarToLocalDateTime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        TimeZone tz = calendar.getTimeZone();
        ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
        return LocalDateTime.ofInstant(calendar.toInstant(), zid);
    }



}
