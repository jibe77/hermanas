package org.jibe77.hermanas.scheduler.sun;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumptionModeManagerTest {

    @Test
    public void testEcoMode() {
        assertTrue(new ConsumptionModeManager().isEcoMode(
                LocalDateTime.of(2020, 12, 05, 12, 00)));
    }
}
