package org.jibe77.hermanas.scheduler.util;

import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.calendar.astro.SolarTime;
import net.time4j.tz.repo.TZDATA;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SunTimeUtilsTest {

    @Test
    public void testTime4j() {
        TZDATA.init();

        SolarTime hamburg = SolarTime.ofLocation(SunTimeUtils.LATITUDE, SunTimeUtils.LONGITUDE);
        Optional<Moment> result = PlainDate.nowInSystemTime().get(hamburg.sunrise());
        System.out.println(result.get().toZonalTimestamp(() -> "Europe/Berlin"));
    }

    /**
     * This test may fail if the dailight saving time is cancel and java is correctly updated.
     */
    @Test
    public void testWinderToSummerTime() {
        LocalDateTime dateTime = LocalDateTime.of(2022, Month.MARCH, 26, 0, 0);
        assertEquals("2022-03-26T06:27:13", SunTimeUtils.computeNextSunrise(dateTime).toString(), "Verify sunrise time is at 06:27 before summer time change.");
        assertEquals("2022-03-26T18:57:34", SunTimeUtils.computeNextSunset(dateTime).toString(), "Verify sunset time is at 18:57 before summer time change.");

        dateTime = LocalDateTime.of(2022, Month.MARCH, 27, 0, 0);
        assertEquals("2022-03-27T07:25:04", SunTimeUtils.computeNextSunrise(dateTime).toString(), "Verify sunrise time is at 07:25 after summer time change.");
        assertEquals("2022-03-27T19:59:06", SunTimeUtils.computeNextSunset(dateTime).toString(), "Verify sunset time is at 19:59 after summer time change.");

        dateTime = LocalDateTime.of(2022, Month.MARCH, 27, 23, 0);
        assertEquals("2022-03-28T07:22:55", SunTimeUtils.computeNextSunrise(dateTime).toString(), "Verify 07:22 the next day if the sunrise is already past.");
        assertEquals("2022-03-28T20:00:37", SunTimeUtils.computeNextSunset(dateTime).toString(), "Verify 20:00 the next day if the sunset is already past");
    }

    /**
     * This test may fail if the dailight saving time is cancel and java is correctly updated.
     */
    @Test
    public void testSummerToWinterTime() {
        LocalDateTime dateTime = LocalDateTime.of(2022, Month.OCTOBER, 29, 0, 0);
        assertEquals("2022-10-29T08:19:25", SunTimeUtils.computeNextSunrise(dateTime).toString(), "Verify the sunrise is at 8:19 before winter time change.");
        assertEquals("2022-10-29T18:21:20", SunTimeUtils.computeNextSunset(dateTime).toString(), "Verify the sunset is at 18:21 before winter time change.");

        dateTime = LocalDateTime.of(2022, Month.OCTOBER, 30, 0, 0);
        assertEquals("2022-10-30T07:21:03", SunTimeUtils.computeNextSunrise(dateTime).toString(), "Verify the sunrise is at 07:21 after winter time change.");
        assertEquals("2022-10-30T17:19:37", SunTimeUtils.computeNextSunset(dateTime).toString(), "Verify the sunrise is at 17:19 after winter time change.");
    }
}
