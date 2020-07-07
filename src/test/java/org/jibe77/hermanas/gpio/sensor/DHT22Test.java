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
                Mockito.anyBoolean(),
                Mockito.anyLong())
            ).thenReturn(new byte[] {1, 2, 3, 4, 10});
        dht22.read();
        assertEquals(25.8, dht22.getHumidity());
        assertEquals(77.2, dht22.getTemperature());
    }
}
