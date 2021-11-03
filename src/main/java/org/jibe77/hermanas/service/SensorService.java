package org.jibe77.hermanas.service;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.controller.sensor.SensorController;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class SensorService {

    SensorController sensorController;

    WeatherClient weatherClient;

    SensorRepository sensorRepository;

    public SensorService(SensorController sensorController, WeatherClient weatherClient, SensorRepository sensorRepository) {
        this.sensorController = sensorController;
        this.weatherClient = weatherClient;
        this.sensorRepository = sensorRepository;
    }

    /**
     * exemple de résultat :
     *
     * {"temperature":7.2,"externalTemperature":5.0,"humidity":99.9,"externalHumidity":93.0,"dateTime":"2021-01-30T15:49:47.993426"}
     *
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/sensor/info")
    public Sensor getInfo() throws IOException {
        Sensor sensor = sensorController.refreshData();
        WeatherInfo weatherInfo = weatherClient.getInfo();
        sensor.setExternalHumidity(weatherInfo.getHumidity());
        sensor.setExternalTemperature(weatherInfo.getTemp());
        return sensor;
    }

    @GetMapping(value = "/sensor/history/today")
    public List<Sensor> getHistoryLastDay() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusDays(1));
    }

    @GetMapping(value = "/sensor/history/week")
    public List<Sensor> getHistoryLastWeek() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusWeeks(1));
    }

    @GetMapping(value = "/sensor/history/month")
    public List<Sensor> getTodayLastMonth() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusMonths(1));
    }

    @GetMapping(value = "/sensor/history/year")
    public List<Sensor> getHistoryYear(@RequestParam(required = false) int year) {
        LocalDateTime startDate = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime endDate = LocalDateTime.now().withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59);
        if (year != 0) {
            startDate = startDate.withYear(year);
            endDate = endDate.withYear(year);
        }
        return sensorRepository.findByDateTimeGreaterThanAndDateTimeGreaterThan(startDate, endDate);
    }

    @GetMapping(value = "/sensor/history/all")
    public Iterable<Sensor> getHistoryAll() {
        return sensorRepository.findAll();
    }
}
