package org.jibe77.hermanas.scheduler.sun;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {SunTimeUtils.class})
public class SunTimeManagerTest {

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    @Value("${suntime.zoneId}")
    private String zoneId;
    private ZoneId zone;

    @Autowired
    SunTimeUtils sunTimeUtils;

    Logger logger = LoggerFactory.getLogger(SunTimeManagerTest.class);

    @BeforeEach
    private void init() {
        zone = ZoneId.of(zoneId);
    }

    /**
     * Test closing door time with gps properties in tokyo.
     */
    @Test
    public void testNextClosingDoorTime() {
        ZonedDateTime dateTime = ZonedDateTime.of(
                2020,
                6,
                20,
                19,
                25,
                0,
                0,
                zone);

        logger.info("The date is converted to the system default zone : {}",
                dateTime.withZoneSameInstant(ZoneId.systemDefault()).toString());
        assertEquals("2020-06-20T19:35:20+09:00[Asia/Tokyo]",
                sunTimeUtils.computeTimeForNextSunsetEvent(dateTime, 15).toString(),
                "search next sunset event time. In this case the sunset is already passed at 19:20 " +
                        "(in the past) but the event is in the futur, so the event is during the same day.");
    }
}
