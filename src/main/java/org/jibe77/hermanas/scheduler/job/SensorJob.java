package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.gpio.sensor.DHT22;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SensorJob {

    private DHT22 dht22;

    Logger logger = LoggerFactory.getLogger(SensorJob.class);

    public SensorJob(DHT22 dht22) {
        this.dht22 = dht22;
    }

    @Scheduled(fixedDelayString = "${sensor.scheduler.delay.in.milliseconds}")
    public void execute() {
        try {
            logger.info("Sensor scheduled job is taking temperature and humidity now.");
            dht22.read();
            logger.info("Temperature {} Humidity {}.", dht22.getTemperature(), dht22.getHumidity());
        } catch (Exception e) {
            logger.error("Can't take temperature and humidity.", e);
        }
    }
}