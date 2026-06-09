package org.jibe77.hermanas.websocket;

import org.jibe77.hermanas.data.entity.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Pushes fresh DHT22 readings (temperature + humidity) onto the STOMP topic
 * {@code /topic/sensor}. Subscribers — currently the Electronics page — display
 * the latest values without polling.
 *
 * <p>The reading itself is computed by {@link org.jibe77.hermanas.service.sensor.SensorService};
 * this controller is only a thin publish helper, mirroring
 * {@link ButtonNotificationController} and {@link NotificationController}.</p>
 */
@Controller
public class SensorNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(SensorNotificationController.class);

    private static final String TOPIC = "/topic/sensor";

    private final MessageSendingOperations<String> wsTemplate;

    public SensorNotificationController(MessageSendingOperations<String> wsTemplate) {
        this.wsTemplate = wsTemplate;
    }

    public void notify(Sensor sensor) {
        if (sensor == null) {
            return;
        }
        SensorReading payload = new SensorReading(
                sensor.getTemperature(),
                sensor.getHumidity(),
                sensor.getDateTime(),
                System.currentTimeMillis());
        logger.debug("notifying on web-socket sensor temp={} hum={}.",
                payload.getTemperature(), payload.getHumidity());
        this.wsTemplate.convertAndSend(TOPIC, payload);
    }

    /**
     * Lightweight DTO sent to the SPA. We intentionally do not send the JPA entity so
     * the database id / external columns stay out of the WebSocket frame.
     */
    public static class SensorReading implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Double temperature;
        private final Double humidity;
        private final LocalDateTime sampledAt;
        /** Epoch millis on the server — easier to consume in Angular than a LocalDateTime. */
        private final long timestamp;

        public SensorReading(Double temperature, Double humidity,
                             LocalDateTime sampledAt, long timestamp) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.sampledAt = sampledAt;
            this.timestamp = timestamp;
        }

        public Double getTemperature() { return temperature; }
        public Double getHumidity() { return humidity; }
        public LocalDateTime getSampledAt() { return sampledAt; }
        public long getTimestamp() { return timestamp; }
    }
}
