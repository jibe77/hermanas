package org.jibe77.hermanas.service;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.controller.sensor.SensorController;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RestController
public class SensorService {

    Logger logger = LoggerFactory.getLogger(SensorService.class);

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
    public List<Sensor> getHistoryYear() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusYears(1));
    }

    @GetMapping(value = "/sensor/history/year/{year}")
    public List<Sensor> getHistoryYear(@PathVariable(name="year") String year) {
        logger.info("fetching history with year parameter : {}.", year);
        LocalDateTime startDate =
                LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0)
                        .withSecond(0).withYear(Integer.valueOf(year));
        LocalDateTime endDate =
                LocalDateTime.now().withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59)
                        .withSecond(59).withYear(Integer.valueOf(year));

        logger.info("start date is {} and end date parameter is {}.", startDate, endDate);
        return sensorRepository.findByDateTimeBetween(startDate, endDate);
    }

    @GetMapping(value = "/sensor/history/{from}/{to}")
    public List<Sensor> getHistory(@PathVariable(name = "from") @DateTimeFormat(pattern = "yyyy-MM-dd") Date from, @PathVariable(name = "from") @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {
        logger.info("fetching history from {} to {}.", from, to);
        return sensorRepository.findByDateTimeBetween(convertToLocalDateTimeViaInstant(from), convertToLocalDateTimeViaInstant(to));

    }

    @GetMapping(value = "/sensor/history/years")
    public List<String> getHistoryYearList() {
        return sensorRepository.getHistoryYearList();
    }

    @GetMapping(value = "/sensor/history/all")
    public Iterable<Sensor> getHistoryAll() {
        return sensorRepository.findAll();
    }

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
