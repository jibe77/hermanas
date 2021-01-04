package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.jibe77.hermanas.controller.sensor.SensorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SensorJob {

    private SensorController sensorController;

    private SensorRepository sensorRepository;

    private WeatherClient weatherClient;

    Logger logger = LoggerFactory.getLogger(SensorJob.class);

    public SensorJob(SensorController sensorController, SensorRepository sensorRepository, WeatherClient weatherClient) {
        this.sensorController = sensorController;
        this.sensorRepository = sensorRepository;
        this.weatherClient = weatherClient;
    }

    @Scheduled(fixedDelayString = "${sensor.scheduler.delay.in.milliseconds}")
    public void execute() {
        try {
            logger.info("Sensor scheduled job is taking temperature and humidity now.");
            Sensor sensor = sensorController.refreshData();

            WeatherInfo weatherInfo = weatherClient.getInfo();
            sensor.setExternalTemperature(weatherInfo.getTemp());
            sensor.setExternalHumidity(weatherInfo.getHumidity());

            sensorRepository.save(sensor);
            logger.info("Temperature {} Humidity {}.", sensor.getTemperature(), sensor.getHumidity());
        } catch (Exception e) {
            logger.error("Can't take temperature and humidity.", e);
        }
    }
}