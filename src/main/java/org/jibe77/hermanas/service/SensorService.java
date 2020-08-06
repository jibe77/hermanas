package org.jibe77.hermanas.service;

import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.gpio.sensor.DHT22;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensorService {

    DHT22 dht22;

    WeatherClient weatherClient;

    public SensorService(DHT22 dht22, WeatherClient weatherClient) {
        this.dht22 = dht22;
        this.weatherClient = weatherClient;
    }

    @GetMapping(value = "/sensor/info")
    public Sensor getInfo() throws Exception {
        Sensor sensor = dht22.refreshData();
        WeatherInfo weatherInfo = weatherClient.getInfo();
        sensor.setExternalHumidity(weatherInfo.getHumidity());
        sensor.setExternalTemperature(weatherInfo.getTemp());
        return sensor;
    }
}
