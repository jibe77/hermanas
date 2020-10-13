package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BirdhouseButtonListenerTest {

    BirdhouseButtonListener birdhouseButtonListener;

    BirdhouseButtonController birdhouseButtonController = Mockito.mock(BirdhouseButtonController.class);

    LightController lightController = Mockito.mock(LightController.class);

    @BeforeEach
    public void setup() {
        birdhouseButtonListener = new BirdhouseButtonListener(birdhouseButtonController, lightController);
    }

    @Test
    void testBirdhouseIsOpenedAndLightIsAlreadyOn() {
        GpioPinDigitalStateChangeEvent event = Mockito.mock(GpioPinDigitalStateChangeEvent.class);
        Mockito.when(event.getState()).thenReturn(PinState.HIGH);
        Mockito.when(lightController.isSwitchedOn()).thenReturn(true);

        birdhouseButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        Mockito.verify(birdhouseButtonController, Mockito.times(1)).setLightHasBeenSwitchedOnByBirdhouseDoor(false);
    }

    @Test
    void testBirdhouseIsOpened() {
        GpioPinDigitalStateChangeEvent event = Mockito.mock(GpioPinDigitalStateChangeEvent.class);
        Mockito.when(event.getState()).thenReturn(PinState.HIGH);
        Mockito.when(lightController.isSwitchedOn()).thenReturn(false);

        birdhouseButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        Mockito.verify(birdhouseButtonController, Mockito.times(1)).setLightHasBeenSwitchedOnByBirdhouseDoor(true);
        Mockito.verify(lightController, Mockito.times(1)).switchOn();
    }

    @Test
    void testBirdhouseIsClosed() {
        GpioPinDigitalStateChangeEvent event = Mockito.mock(GpioPinDigitalStateChangeEvent.class);
        Mockito.when(event.getState()).thenReturn(PinState.LOW);
        Mockito.when(birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor()).thenReturn(true);

        birdhouseButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        Mockito.verify(birdhouseButtonController, Mockito.times(1)).setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        Mockito.verify(lightController, Mockito.times(1)).switchOff();
    }

    @Test
    void testBirdhouseIsClosedAndLightIsAlreadyOn() {
        GpioPinDigitalStateChangeEvent event = Mockito.mock(GpioPinDigitalStateChangeEvent.class);
        Mockito.when(event.getState()).thenReturn(PinState.LOW);
        Mockito.when(birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor()).thenReturn(false);

        birdhouseButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        Mockito.verify(birdhouseButtonController, Mockito.times(1)).setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        Mockito.verify(lightController, Mockito.times(0)).switchOff();
    }
}
