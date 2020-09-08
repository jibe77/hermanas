package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.jibe77.hermanas.controller.sensor.DHT22;
import org.jibe77.hermanas.data.entity.Sensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BirdhouseButtonControllerTest {

    BirdhouseButtonController birdhouseButtonController;

    GpioHermanasController gpioHermanasController = Mockito.mock(GpioHermanasController.class);

    LightController lightController = Mockito.mock(LightController.class);

    @BeforeEach
    public void setup() {
        birdhouseButtonController = new BirdhouseButtonController(gpioHermanasController, lightController);
    }

    @Test
    void testInit() {
        GpioPinDigitalInput gpioPinDigitalInput = Mockito.mock(GpioPinDigitalInput.class);
        Mockito.when(gpioHermanasController.provisionInput(Mockito.anyInt())).thenReturn(gpioPinDigitalInput);
        birdhouseButtonController.setButton(null);

        birdhouseButtonController.init();

        Mockito.verify(gpioHermanasController, Mockito.times(1)).provisionInput(Mockito.anyInt());
        Mockito.verify(gpioPinDigitalInput, Mockito.times(1)).setShutdownOptions(true);
        Mockito.verify(gpioPinDigitalInput, Mockito.times(1)).addListener(Mockito.any(BirdhouseButtonListener.class));
    }

    @Test
    void testInitWithButtonAlreadyDefined() {
        GpioPinDigitalInput gpioPinDigitalInput = Mockito.mock(GpioPinDigitalInput.class);
        birdhouseButtonController.setButton(gpioPinDigitalInput);

        birdhouseButtonController.init();

        Mockito.verify(gpioHermanasController, Mockito.times(0)).provisionInput(Mockito.anyInt());
        Mockito.verify(gpioPinDigitalInput, Mockito.times(0)).setShutdownOptions(true);
        Mockito.verify(gpioPinDigitalInput, Mockito.times(0)).addListener(Mockito.any(BirdhouseButtonListener.class));
    }

    @Test
    void testDestroy() {
        GpioPinDigitalInput gpioPinDigitalInput = Mockito.mock(GpioPinDigitalInput.class);
        Mockito.when(gpioHermanasController.provisionInput(Mockito.anyInt())).thenReturn(gpioPinDigitalInput);
        birdhouseButtonController.setButton(gpioPinDigitalInput);

        birdhouseButtonController.tearDown();

        Mockito.verify(gpioHermanasController, Mockito.times(1)).unprovisionPin(Mockito.any(GpioPin.class));
        Mockito.verify(gpioPinDigitalInput, Mockito.times(1)).removeAllListeners();
    }

    @Test
    void testDestroyWithoutButton() {
        birdhouseButtonController.setButton(null);

        birdhouseButtonController.tearDown();

        Mockito.verify(gpioHermanasController, Mockito.times(0)).unprovisionPin(Mockito.any(GpioPin.class));
    }
}
