package org.jibe77.hermanas.service.birdhouse;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.service.light.LightService;
import org.jibe77.hermanas.websocket.ButtonNotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Assert;

class BirdhouseButtonControllerTest {

    BirdhouseButtonService birdhouseButtonService;

    GpioHermanasService gpioHermanasService = Mockito.mock(GpioHermanasService.class);

    LightService lightService = Mockito.mock(LightService.class);

    ButtonNotificationController buttonNotificationController = Mockito.mock(ButtonNotificationController.class);

    @BeforeEach
    public void setup() {
        birdhouseButtonService = new BirdhouseButtonService(gpioHermanasService, lightService, buttonNotificationController);
    }

    @Test
    void testInit() {
        DigitalInput gpioPinDigitalInput = Mockito.mock(DigitalInput.class);
        Mockito.when(gpioHermanasService.provisionInput(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(gpioPinDigitalInput);
        birdhouseButtonService.setButton(null);

        birdhouseButtonService.initButton();

        Mockito.verify(gpioHermanasService, Mockito.times(1)).provisionInput(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    void testInitWithButtonAlreadyDefined() {
        DigitalInput gpioPinDigitalInput = Mockito.mock(DigitalInput.class);
        birdhouseButtonService.setButton(gpioPinDigitalInput);

        birdhouseButtonService.initButton();

        Mockito.verify(gpioHermanasService, Mockito.times(0)).provisionInput(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }


    @Test
    void testBirdhouseIsOpenedAndLightIsAlreadyOn() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.HIGH);
        Mockito.when(lightService.getStatus()).thenReturn(new Status(StatusEnum.ON, -1));

        birdhouseButtonService.manageEvent(event);
        Assert.isTrue(!birdhouseButtonService.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
    }

    @Test
    void testBirdhouseIsOpened() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.HIGH);
        Mockito.when(lightService.getStatus()).thenReturn(new Status(StatusEnum.OFF, -1));

        birdhouseButtonService.manageEvent(event);

        Assert.isTrue(birdhouseButtonService.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is switched on by birdhouse door.");
        Mockito.verify(lightService, Mockito.times(1)).switchOn();
    }

    @Test
    void testBirdhouseIsClosed() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.LOW);
        birdhouseButtonService.setLightHasBeenSwitchedOnByBirdhouseDoor(true);

        birdhouseButtonService.manageEvent(event);

        Assert.isTrue(!birdhouseButtonService.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
        Mockito.verify(lightService, Mockito.times(1)).switchOff();
    }

    @Test
    void testBirdhouseIsClosedAndLightIsAlreadyOn() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.LOW);
        birdhouseButtonService.setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        // Le relâchement non réclamé par le bouton relit l'état de la lumière pour
        // tracer le cas « encore allumée par quelqu'un d'autre » : sans ce stub le
        // mock renvoie null et manageEvent casse sur un NPE.
        Mockito.when(lightService.getStatus()).thenReturn(new Status(StatusEnum.OFF, -1));

        birdhouseButtonService.manageEvent(event);

        Assert.isTrue(!birdhouseButtonService.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
        Mockito.verify(lightService, Mockito.times(0)).switchOff();
    }

    /**
     * Relâchement du bouton alors que la lumière est encore allumée sans avoir été
     * réclamée par le bouton (une capture photo la tient, par exemple) : le bouton
     * ne doit pas l'éteindre, il se contente de tracer l'anomalie.
     */
    @Test
    void testBirdhouseIsClosedAndLightIsStillOnButNotClaimedByTheButton() {
        DigitalStateChangeEvent event = Mockito.mock(DigitalStateChangeEvent.class);
        Mockito.when(event.state()).thenReturn(DigitalState.LOW);
        birdhouseButtonService.setLightHasBeenSwitchedOnByBirdhouseDoor(false);
        Mockito.when(lightService.getStatus()).thenReturn(new Status(StatusEnum.ON, -1));

        birdhouseButtonService.manageEvent(event);

        Assert.isTrue(!birdhouseButtonService.isLightHasBeenSwitchedOnByBirdhouseDoor(),
                "The light is not switched on by birdhouse door.");
        Mockito.verify(lightService, Mockito.times(0)).switchOff();
    }
}
