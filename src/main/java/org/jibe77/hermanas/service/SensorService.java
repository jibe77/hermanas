package org.jibe77.hermanas.service;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.controller.sensor.SensorController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class SensorService {

    SensorController sensorController;

    WeatherClient weatherClient;

    public SensorService(SensorController sensorController, WeatherClient weatherClient) {
        this.sensorController = sensorController;
        this.weatherClient = weatherClient;
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
}
