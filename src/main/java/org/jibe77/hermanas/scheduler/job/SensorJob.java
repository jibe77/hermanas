package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.jibe77.hermanas.gpio.sensor.DHT22;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SensorJob {

    private DHT22 dht22;

    private SensorRepository sensorRepository;

    Logger logger = LoggerFactory.getLogger(SensorJob.class);

    public SensorJob(DHT22 dht22, SensorRepository sensorRepository) {
        this.dht22 = dht22;
        this.sensorRepository = sensorRepository;
    }

    @Scheduled(fixedDelayString = "${sensor.scheduler.delay.in.milliseconds}")
    public void execute() {
        try {
            logger.info("Sensor scheduled job is taking temperature and humidity now.");
            Sensor sensor = dht22.refreshData();
            sensor.setTemperature(sensor.getTemperature());
            sensor.setHumidity(sensor.getHumidity());
            sensor.setDateTime(LocalDateTime.now());
            sensorRepository.save(sensor);
            logger.info("Temperature {} Humidity {}.", sensor.getTemperature(), sensor.getHumidity());
        } catch (Exception e) {
            logger.error("Can't take temperature and humidity.", e);
        }
    }
}