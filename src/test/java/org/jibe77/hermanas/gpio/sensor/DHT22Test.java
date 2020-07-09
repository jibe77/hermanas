package org.jibe77.hermanas.gpio.sensor;


import com.pi4j.wiringpi.Gpio;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {DHT22.class})
public class DHT22Test {

    @Autowired
    DHT22 dht22;

    @MockBean
    GpioHermanasController gpioHermanasController;

    @Test
    public void testReadSensor() throws Exception {
        Mockito.when(gpioHermanasController.fetchData(
                Mockito.anyInt(),
                Mockito.any())
            ).thenReturn(50);
        assertEquals(0, dht22.getHumidity());
        assertEquals(0, dht22.getTemperature());
    }

    @Test
    public void testReadSensorWithRefresh() throws Exception {
        Mockito.when(gpioHermanasController.fetchData(
                Mockito.anyInt(),
                Mockito.any())
        ).thenReturn(50);
        dht22.refreshData();
        assertEquals(0, dht22.getHumidity());
        assertEquals(0, dht22.getTemperature());
    }
}
