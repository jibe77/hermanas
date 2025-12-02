package org.jibe77.hermanas.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Centralized metrics tracking for Hermanas chicken coop automation system.
 *
 * <p>Provides methods to record metrics for all major operations:</p>
 * <ul>
 *   <li><b>Door metrics:</b> open/close operations, failures, duration</li>
 *   <li><b>Sensor metrics:</b> current temperature and humidity readings</li>
 *   <li><b>Appliance metrics:</b> light/fan/music switching operations</li>
 *   <li><b>Camera metrics:</b> picture captures, streaming sessions</li>
 *   <li><b>Config metrics:</b> configuration changes</li>
 * </ul>
 *
 * <p>All metrics are prefixed with {@code hermanas.*} and can be accessed via
 * Spring Boot Actuator at {@code /actuator/metrics/hermanas.*}</p>
 *
 * @see MeterRegistry
 */
public class HermanasMetrics {

    private static final Logger logger = LoggerFactory.getLogger(HermanasMetrics.class);

    private final MeterRegistry registry;

    // Door metrics
    private final Counter doorOpenCounter;
    private final Counter doorCloseCounter;
    private final Counter doorFailureCounter;
    private final Timer doorOperationTimer;
    private final AtomicInteger doorPosition;

    // Sensor metrics
    private final AtomicReference<Double> currentTemperature;
    private final AtomicReference<Double> currentHumidity;

    // Appliance metrics
    private final Counter lightSwitchCounter;
    private final Counter fanSwitchCounter;
    private final Counter musicPlayCounter;

    // Camera metrics
    private final Counter pictureCaptureCounter;
    private final Counter streamingSessionCounter;

    // Config metrics
    private final Counter configChangeCounter;

    public HermanasMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Initialize door metrics
        this.doorOpenCounter = Counter.builder("hermanas.door.operations")
                .tag("operation", "open")
                .description("Number of door open operations")
                .register(registry);

        this.doorCloseCounter = Counter.builder("hermanas.door.operations")
                .tag("operation", "close")
                .description("Number of door close operations")
                .register(registry);

        this.doorFailureCounter = Counter.builder("hermanas.door.failures")
                .description("Number of door operation failures")
                .register(registry);

        this.doorOperationTimer = Timer.builder("hermanas.door.operation.duration")
                .description("Duration of door operations (open/close)")
                .register(registry);

        this.doorPosition = new AtomicInteger(0); // 0=unknown, 1=open, 2=closed
        Gauge.builder("hermanas.door.position", doorPosition, AtomicInteger::get)
                .description("Current door position (0=unknown, 1=open, 2=closed)")
                .register(registry);

        // Initialize sensor metrics
        this.currentTemperature = new AtomicReference<>(0.0);
        Gauge.builder("hermanas.sensor.temperature", currentTemperature, ref -> ref.get())
                .description("Current temperature in Celsius")
                .baseUnit("celsius")
                .register(registry);

        this.currentHumidity = new AtomicReference<>(0.0);
        Gauge.builder("hermanas.sensor.humidity", currentHumidity, ref -> ref.get())
                .description("Current humidity percentage")
                .baseUnit("percent")
                .register(registry);

        // Initialize appliance metrics
        this.lightSwitchCounter = Counter.builder("hermanas.appliance.switches")
                .tag("appliance", "light")
                .description("Number of light switch operations")
                .register(registry);

        this.fanSwitchCounter = Counter.builder("hermanas.appliance.switches")
                .tag("appliance", "fan")
                .description("Number of fan switch operations")
                .register(registry);

        this.musicPlayCounter = Counter.builder("hermanas.appliance.switches")
                .tag("appliance", "music")
                .description("Number of music play operations")
                .register(registry);

        // Initialize camera metrics
        this.pictureCaptureCounter = Counter.builder("hermanas.camera.captures")
                .description("Number of pictures captured")
                .register(registry);

        this.streamingSessionCounter = Counter.builder("hermanas.camera.streams")
                .description("Number of streaming sessions started")
                .register(registry);

        // Initialize config metrics
        this.configChangeCounter = Counter.builder("hermanas.config.changes")
                .description("Number of configuration changes")
                .register(registry);

        logger.info("Hermanas metrics initialized successfully");
    }

    // ============================================================================
    // Door Metrics
    // ============================================================================

    /**
     * Records a door open operation.
     * Increments the door open counter and updates position gauge.
     */
    public void recordDoorOpen() {
        doorOpenCounter.increment();
        doorPosition.set(1); // 1 = open
        logger.debug("Recorded door open operation");
    }

    /**
     * Records a door close operation.
     * Increments the door close counter and updates position gauge.
     */
    public void recordDoorClose() {
        doorCloseCounter.increment();
        doorPosition.set(2); // 2 = closed
        logger.debug("Recorded door close operation");
    }

    /**
     * Records a door operation failure.
     *
     * @param operationType the type of operation that failed (e.g., "open", "close")
     */
    public void recordDoorFailure(String operationType) {
        doorFailureCounter.increment();
        Counter.builder("hermanas.door.failures.by.type")
                .tag("operation", operationType)
                .description("Door failures by operation type")
                .register(registry)
                .increment();
        logger.debug("Recorded door failure for operation: {}", operationType);
    }

    /**
     * Returns a timer for measuring door operation duration.
     * Use with try-with-resources or manual start/stop.
     *
     * @return Timer.Sample that can be stopped to record the duration
     */
    public Timer.Sample startDoorOperationTimer() {
        return Timer.start(registry);
    }

    /**
     * Stops a door operation timer and records the duration.
     *
     * @param sample the timer sample to stop
     */
    public void stopDoorOperationTimer(Timer.Sample sample) {
        sample.stop(doorOperationTimer);
    }

    /**
     * Updates the door position gauge based on status.
     *
     * @param status the current door status
     */
    public void updateDoorPosition(DoorStatusEnum status) {
        switch (status) {
            case OPENED:
                doorPosition.set(1);
                break;
            case CLOSED:
                doorPosition.set(2);
                break;
            default:
                doorPosition.set(0); // unknown
                break;
        }
    }

    // ============================================================================
    // Sensor Metrics
    // ============================================================================

    /**
     * Records current sensor readings (temperature and humidity).
     *
     * @param temperature current temperature in Celsius
     * @param humidity current humidity percentage
     */
    public void recordSensorReading(double temperature, double humidity) {
        currentTemperature.set(temperature);
        currentHumidity.set(humidity);
        logger.debug("Recorded sensor reading: {}°C, {}%", temperature, humidity);
    }

    // ============================================================================
    // Appliance Metrics
    // ============================================================================

    /**
     * Records a light switch operation.
     */
    public void recordLightSwitch() {
        lightSwitchCounter.increment();
        logger.debug("Recorded light switch operation");
    }

    /**
     * Records a fan switch operation.
     */
    public void recordFanSwitch() {
        fanSwitchCounter.increment();
        logger.debug("Recorded fan switch operation");
    }

    /**
     * Records a music play operation.
     */
    public void recordMusicPlay() {
        musicPlayCounter.increment();
        logger.debug("Recorded music play operation");
    }

    // ============================================================================
    // Camera Metrics
    // ============================================================================

    /**
     * Records a picture capture operation.
     */
    public void recordPictureCapture() {
        pictureCaptureCounter.increment();
        logger.debug("Recorded picture capture");
    }

    /**
     * Records a streaming session start.
     */
    public void recordStreamingSession() {
        streamingSessionCounter.increment();
        logger.debug("Recorded streaming session");
    }

    // ============================================================================
    // Configuration Metrics
    // ============================================================================

    /**
     * Records a configuration change.
     *
     * @param configKey the configuration key that was changed
     */
    public void recordConfigChange(String configKey) {
        configChangeCounter.increment();
        Counter.builder("hermanas.config.changes.by.key")
                .tag("key", configKey)
                .description("Configuration changes by key")
                .register(registry)
                .increment();
        logger.debug("Recorded configuration change for key: {}", configKey);
    }
}
