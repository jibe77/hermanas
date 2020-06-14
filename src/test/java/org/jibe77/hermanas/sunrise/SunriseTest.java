package org.jibe77.hermanas.sunrise;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

public class SunriseTest {

    @Test
    public void testSunRiseTime() {
        Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), 48.85837, 2.294481);
        System.out.println("Sunrise at: " + sunriseSunset[0].getTime());
        System.out.println("Sunset at: " + sunriseSunset[1].getTime());
    }
}
