package org.jibe77.hermanas.scheduler.util;


import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SunTimeTest {

    /**
     * This test may fail if the dailight saving time is cancel and java is correctly updated.
     */
    @Test
    public void testWinderToSummerTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.MARCH, 26, 0, 0);
        Date nextSunsetTime = SunTime.getNextSunsetTime(calendar.getTime());
        Date nextSunriseTime = SunTime.getNextSunriseTime(calendar.getTime());
        System.out.println("Sunrise before summer time at: " + nextSunriseTime);
        System.out.println("Sunset before summer time at: " + nextSunsetTime);
        // Verify 06:27
        calendar.setTime(nextSunriseTime);
        assertEquals(6, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(27, calendar.get(Calendar.MINUTE));
        // Verify 18:57
        calendar.setTime(nextSunsetTime);
        assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(57, calendar.get(Calendar.MINUTE));

        calendar.set(2022, Calendar.MARCH, 27, 0, 0);
        nextSunsetTime = SunTime.getNextSunsetTime(calendar.getTime());
        nextSunriseTime = SunTime.getNextSunriseTime(calendar.getTime());
        System.out.println("Sunrise after summer time at: " + nextSunriseTime);
        System.out.println("Sunset after summer time at: " + nextSunsetTime);
        // Verify 07:25
        calendar.setTime(nextSunriseTime);
        assertEquals(7, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(25, calendar.get(Calendar.MINUTE));
        // Verify 19:59
        calendar.setTime(nextSunsetTime);
        assertEquals(19, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, calendar.get(Calendar.MINUTE));

        calendar.set(2022, Calendar.MARCH, 27, 23, 0);
        nextSunsetTime = SunTime.getNextSunsetTime(calendar.getTime());
        nextSunriseTime = SunTime.getNextSunriseTime(calendar.getTime());
        System.out.println("Sunrise next day after summer time at: " + nextSunriseTime);
        System.out.println("Sunset next day after summer time at: " + nextSunsetTime);
        // Verify 07:25
        calendar.setTime(nextSunriseTime);
        assertEquals(7, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(22, calendar.get(Calendar.MINUTE));
        // Verify 19:59
        calendar.setTime(nextSunsetTime);
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    /**
     * This test may fail if the dailight saving time is cancel and java is correctly updated.
     */
    @Test
    public void testSummerToWinterTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.OCTOBER, 29, 0, 0);
        Date nextSunsetTime = SunTime.getNextSunsetTime(calendar.getTime());
        Date nextSunriseTime = SunTime.getNextSunriseTime(calendar.getTime());
        System.out.println("Sunrise before winter time at: " + nextSunriseTime);
        System.out.println("Sunset before winter time at: " + nextSunsetTime);
        // Verify 8:19
        calendar.setTime(nextSunriseTime);
        assertEquals(8, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(19, calendar.get(Calendar.MINUTE));
        // Verify 18:21
        calendar.setTime(nextSunsetTime);
        assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(21, calendar.get(Calendar.MINUTE));

        calendar.set(2022, Calendar.OCTOBER, 30, 0, 0);
        nextSunsetTime = SunTime.getNextSunsetTime(calendar.getTime());
        nextSunriseTime = SunTime.getNextSunriseTime(calendar.getTime());
        System.out.println("Sunrise after winter time at: " + nextSunriseTime);
        System.out.println("Sunset after winter time at: " + nextSunsetTime);
        // Verify 07:21
        calendar.setTime(nextSunriseTime);
        assertEquals(7, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(21, calendar.get(Calendar.MINUTE));
        // Verify 17:19
        calendar.setTime(nextSunsetTime);
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(19, calendar.get(Calendar.MINUTE));
    }
}
