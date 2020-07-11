package org.jibe77.hermanas.service;

import org.jibe77.hermanas.gpio.sensor.DHT22;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensorService {

    DHT22 dht22;

    public SensorService(DHT22 dht22) {
        this.dht22 = dht22;
    }

    @GetMapping(value = "/sensor/temperature")
    public Double getTemperature() throws Exception {
        dht22.refreshData();
        return dht22.refreshData().getTemperature();
    }

    @GetMapping(value = "/sensor/humidity")
    public Double getHumidity() throws Exception {
        dht22.refreshData();
        return dht22.refreshData().getHumidity();
    }
}
