package org.jibe77.hermanas.controller.sensor;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {DHT22.class})
class DHT22Test {

    @Autowired
    DHT22 dht22;

    @MockBean
    GpioHermanasController gpioHermanasController;

    @Test
    void testReadSensorWithRefresh() throws Exception {
        Sensor sensor = dht22.refreshData();
        assertEquals(57.6, sensor.getHumidity());
        assertEquals(24.9, sensor.getTemperature());
    }

    @Test
    void testParseSensorReturnedValue() {
        Sensor sensor = dht22.parseSensorReturnedValue("Temp=2.9* Humidity=7.6%");
        assertEquals(7.6, sensor.getHumidity());
        assertEquals(2.9, sensor.getTemperature());
    }

    @Test
    void testParseSensorReturnedValueMinus() {
        Sensor sensor = dht22.parseSensorReturnedValue("Temp=-2.9* Humidity=7.6%");
        assertEquals(7.6, sensor.getHumidity());
        assertEquals(-2.9, sensor.getTemperature());
    }
}
