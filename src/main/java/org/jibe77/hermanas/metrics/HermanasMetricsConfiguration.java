package org.jibe77.hermanas.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Micrometer metrics configuration for Hermanas chicken coop automation.
 *
 * <p>Provides custom metrics for observability of key operations:</p>
 * <ul>
 *   <li>Door operations (open, close, status checks)</li>
 *   <li>Sensor readings (temperature, humidity)</li>
 *   <li>Appliance control (light, fan, music)</li>
 *   <li>Camera operations (picture capture, streaming)</li>
 *   <li>Configuration changes</li>
 * </ul>
 *
 * <p>Metrics are exposed via Spring Boot Actuator at {@code /actuator/metrics/*}</p>
 *
 * @see io.micrometer.core.instrument.MeterRegistry
 */
@Configuration
public class HermanasMetricsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HermanasMetricsConfiguration.class);

    /**
     * Creates and configures the Hermanas metrics service bean.
     *
     * @param registry the Micrometer meter registry
     * @return configured HermanasMetrics instance
     */
    @Bean
    public HermanasMetrics hermanasMetrics(MeterRegistry registry) {
        logger.info("Initializing Hermanas custom metrics");
        return new HermanasMetrics(registry);
    }
}
