package org.jibe77.hermanas.scheduler.util;

import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class SunTime {

    public final static double LATITUDE = 49.37103491327111;
    public final static double LONGITUDE = 6.139976893987993;

    protected static Date getNextSunsetTime(Date date) {
        Date currentDaySunset = generateCurrentDay(date)[1].getTime();
        if(date.after(currentDaySunset)) {
            return generateNextDay(date)[1].getTime();
        } else {
            return currentDaySunset;
        }
    }

    public static Date getNextSunsetTime() {
        return getNextSunsetTime(Calendar.getInstance().getTime());
    }

    protected static Date getNextSunriseTime(Date date) {
        Date currentDaySunrise = generateCurrentDay(date)[0].getTime();
        if(date.after(currentDaySunrise)) {
            return generateNextDay(date)[0].getTime();
        } else {
            return currentDaySunrise;
        }
    }

    public static Date getNextSunriseTime() {
        return getNextSunriseTime(Calendar.getInstance().getTime());
    }

    private static Calendar[] generateCurrentDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(
                cal,
                SunTime.LATITUDE,
                SunTime.LONGITUDE);
    }

    private static Calendar[] generateNextDay(Date date) {
        Calendar nextDay = Calendar.getInstance();
        nextDay.setTime(date);
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        return generateCurrentDay(nextDay.getTime());
    }

}
