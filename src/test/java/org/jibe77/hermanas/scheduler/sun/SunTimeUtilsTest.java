package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.service.config.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {SunTimeUtils.class})
class SunTimeUtilsTest {

    @MockitoBean
    ConfigService configService;

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    @Value("${suntime.zoneId}")
    private String zoneId;
    private ZoneId zone;
    private ZonedDateTime dateTime;

    @Autowired
    SunTimeUtils sunTimeUtils;

    Logger logger = LoggerFactory.getLogger(SunTimeUtilsTest.class);

    @BeforeEach
    void init() {
        // SunTimeUtils now reads coordinates through ConfigService instead of its own
        // @Value fields, so route the mock to the same property values the test
        // properties file already provides (Tokyo for these expectations).
        Mockito.when(configService.getLatitude()).thenReturn(latitude);
        Mockito.when(configService.getLongitude()).thenReturn(longitude);

        zone = ZoneId.of(zoneId);
        dateTime = ZonedDateTime.of(
                2020,
                6,
                20,
                19,
                25,
                0,
                0,
                zone);
    }

    @Test
    void testSunsetAfterDawn() {
        logger.info("The date is converted to the system default zone : {}",
                dateTime.withZoneSameInstant(ZoneId.systemDefault()));
        assertEquals("2020-06-20T19:20:20+09:00[Asia/Tokyo]",
                sunTimeUtils.computeCurrentDaySunset(dateTime).toString(),
                "search next sunset event time. In this case the sunset is at 19:20.");
    }


    /**
     * Test closing door time with gps properties in tokyo.
     */
    @Test
    void testNextClosingDoorTime() {
        logger.info("The date is converted to the system default zone : {}",
                dateTime.withZoneSameInstant(ZoneId.systemDefault()));
        assertEquals("2020-06-20T19:35:20+09:00[Asia/Tokyo]",
                sunTimeUtils.computeTimeForNextSunsetEvent(dateTime, 15).toString(),
                "search next sunset event time. In this case the sunset is already passed at 19:20 " +
                        "(in the past) but the event is in the futur, so the event is during the same day.");
    }
}
