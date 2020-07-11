package org.jibe77.hermanas.gpio.sensor;

import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {DHT22.class})
public class DHT22Test {

    @Autowired
    DHT22 dht22;

    @MockBean
    GpioHermanasController gpioHermanasController;

    @Test
    public void testReadSensorWithRefresh() throws Exception {
        dht22.refreshData();
        assertEquals(57.6, dht22.getHumidity());
        assertEquals(24.9, dht22.getTemperature());
    }
}
