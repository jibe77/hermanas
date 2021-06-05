package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.jibe77.hermanas.controller.sensor.SensorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class PeriodicJob {

    private SensorController sensorController;

    private SensorRepository sensorRepository;

    private WeatherClient weatherClient;

    WifiController wifiController;

    Logger logger = LoggerFactory.getLogger(PeriodicJob.class);

    public PeriodicJob(SensorController sensorController, SensorRepository sensorRepository, WeatherClient weatherClient, WifiController wifiController) {
        this.sensorController = sensorController;
        this.sensorRepository = sensorRepository;
        this.weatherClient = weatherClient;
        this.wifiController = wifiController;
    }

    @Scheduled(fixedDelayString = "${sensor.scheduler.delay.in.milliseconds}")
    public void execute() {
        boolean initialWifiStatus = wifiController.wifiCardIsEnabled();
        // make the wifi available
        wifiController.turnOn();
        try {
            logger.info("Sensor scheduled job is taking temperature and humidity now.");
            Sensor sensor = sensorController.refreshData();
            WeatherInfo weatherInfo = getWeatherInfo();
            sensor.setExternalTemperature(weatherInfo.getTemp());
            sensor.setExternalHumidity(weatherInfo.getHumidity());

            sensorRepository.save(sensor);
            logger.info("Temperature {} Humidity {}.", sensor.getTemperature(), sensor.getHumidity());
        } catch (Exception e) {
            logger.error("Can't take temperature and humidity.", e);
        } finally {
            if (!initialWifiStatus) {
                logger.info("weather client is disabling the wifi card after a request.");
                wifiController.turnOff();
            }
        }
    }

    private WeatherInfo getWeatherInfo() {
        try {
            return weatherClient.getInfo();
        } catch (ResourceAccessException e) {
            logger.error("Can't fetch external temperature.", e);
            return new WeatherInfo();
        }
    }
}
