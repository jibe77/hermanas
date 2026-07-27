package org.jibe77.hermanas.service.sensor;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {SensorService.class})
class SensorControllerTest {

    @Autowired
    SensorService sensorService;

    @MockitoBean
    GpioHermanasService gpioHermanasService;

    @Test
    void testReadSensorWithRefresh() throws Exception {
        Sensor sensor = sensorService.refreshData();
        assertEquals(57.6, sensor.getHumidity());
        assertEquals(24.9, sensor.getTemperature());
    }

    @Test
    void testParseSensorReturnedValue() {
        Sensor sensor = sensorService.parseSensorReturnedValue("Temp=2.9* Humidity=7.6%");
        assertEquals(7.6, sensor.getHumidity());
        assertEquals(2.9, sensor.getTemperature());
    }

    @Test
    void testParseSensorReturnedValueMinus() {
        Sensor sensor = sensorService.parseSensorReturnedValue("Temp=-2.9* Humidity=7.6%");
        assertEquals(7.6, sensor.getHumidity());
        assertEquals(-2.9, sensor.getTemperature());
    }
}
