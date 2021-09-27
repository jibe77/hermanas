package org.jibe77.hermanas.controller.birdhouse;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.light.LightController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Assert;

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
        DigitalInput gpioPinDigitalInput = Mockito.mock(DigitalInput.class);
        Mockito.when(gpioHermanasController.provisionInput(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(gpioPinDigitalInput);
        birdhouseButtonController.setButton(null);

        birdhouseButtonController.initButton();

        Mockito.verify(gpioHermanasController, Mockito.times(1)).provisionInput(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    void testInitWithButtonAlreadyDefined() {
        DigitalInput gpioPinDigitalInput = Mockito.mock(DigitalInput.class);
        birdhouseButtonController.setButton(gpioPinDigitalInput);

        birdhouseButtonController.initButton();

        Mockito.verify(gpioHermanasController, Mockito.times(0)).provisionInput(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }


    @Test
    void testBirdhouseIsOpenedAndLightIsAlreadyOn() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.HIGH);
        Mockito.when(lightController.getStatus()).thenReturn(new Status(StatusEnum.ON, -1));

        birdhouseButtonController.manageEvent(event);
        Assert.isTrue(!birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
    }

    @Test
    void testBirdhouseIsOpened() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.HIGH);
        Mockito.when(lightController.getStatus()).thenReturn(new Status(StatusEnum.OFF, -1));

        birdhouseButtonController.manageEvent(event);

        Assert.isTrue(birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is switched on by birdhouse door.");
        Mockito.verify(lightController, Mockito.times(1)).switchOn();
    }

    @Test
    void testBirdhouseIsClosed() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.LOW);
        birdhouseButtonController.setLightHasBeenSwitchedOnByBirdhouseDoor(true);

        birdhouseButtonController.manageEvent(event);

        Assert.isTrue(!birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
        Mockito.verify(lightController, Mockito.times(1)).switchOff();
    }

    @Test
    void testBirdhouseIsClosedAndLightIsAlreadyOn() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.LOW);
        birdhouseButtonController.setLightHasBeenSwitchedOnByBirdhouseDoor(false);

        birdhouseButtonController.manageEvent(event);

        Assert.isTrue(!birdhouseButtonController.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
        Mockito.verify(lightController, Mockito.times(0)).switchOff();
    }
}
