package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.jibe77.hermanas.service.sensor.SensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
@Profile("gpio-rpi")
public class PeriodicJob {

    private SensorService sensorService;

    private SensorRepository sensorRepository;

    private WeatherClient weatherClient;

    WifiService wifiService;

    private static final Logger logger = LoggerFactory.getLogger(PeriodicJob.class);

    public PeriodicJob(SensorService sensorService, SensorRepository sensorRepository, WeatherClient weatherClient, WifiService wifiService) {
        this.sensorService = sensorService;
        this.sensorRepository = sensorRepository;
        this.weatherClient = weatherClient;
        this.wifiService = wifiService;
    }

    @Scheduled(fixedDelayString = "${sensor.scheduler.delay.in.milliseconds}")
    public void execute() {
        boolean initialWifiStatus = wifiService.wifiCardIsEnabled();
        // make the wifi available
        wifiService.turnOn();
        try {
            logger.info("Sensor scheduled job is taking temperature and humidity now.");
            Sensor sensor = sensorService.refreshData();
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
                wifiService.turnOff();
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
