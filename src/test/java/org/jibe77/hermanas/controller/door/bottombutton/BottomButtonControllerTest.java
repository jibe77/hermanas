package org.jibe77.hermanas.controller.door.bottombutton;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class BottomButtonControllerTest {

    BottomButtonController bottomButtonController;

    GpioHermanasController gpioHermanasController = mock(GpioHermanasController.class);

    ServoMotorController servoMotorController = mock(ServoMotorController.class);

    @BeforeEach
    public void setup() {
        bottomButtonController = new BottomButtonController(servoMotorController, gpioHermanasController);
    }

    @Test
    void testProvisionButton() {
        GpioPinDigitalInput gpioPinDigitalInput = mock(GpioPinDigitalInput.class);
        when(gpioHermanasController.provisionInput(anyInt())).thenReturn(gpioPinDigitalInput);
        bottomButtonController.setBottomButton(null);

        bottomButtonController.provisionButton();

        verify(gpioHermanasController, times(1)).provisionInput(anyInt());
        verify(gpioPinDigitalInput, times(1)).setShutdownOptions(true);
        verify(gpioPinDigitalInput, times(1)).addListener(any(BottomButtonListener.class));
    }

    @Test
    void testInitWithButtonAlreadyDefined() {
        GpioPinDigitalInput gpioPinDigitalInput = mock(GpioPinDigitalInput.class);
        bottomButtonController.setBottomButton(gpioPinDigitalInput);

        bottomButtonController.provisionButton();

        verify(gpioHermanasController, times(0)).provisionInput(anyInt());
        verify(gpioPinDigitalInput, times(0)).setShutdownOptions(true);
        verify(gpioPinDigitalInput, times(0)).addListener(any(BottomButtonListener.class));
    }

    @Test
    void testDestroy() {
        GpioPinDigitalInput gpioPinDigitalInput = mock(GpioPinDigitalInput.class);
        when(gpioHermanasController.provisionInput(anyInt())).thenReturn(gpioPinDigitalInput);
        bottomButtonController.setBottomButton(gpioPinDigitalInput);

        bottomButtonController.unprovisionButton();

        verify(gpioHermanasController, times(1)).unprovisionPin(any(GpioPin.class));
        verify(gpioPinDigitalInput, times(1)).removeAllListeners();
    }

    @Test
    void testDestroyWithoutButton() {
        bottomButtonController.setBottomButton(null);

        bottomButtonController.unprovisionButton();

        verify(gpioHermanasController, times(0)).unprovisionPin(any(GpioPin.class));
    }
}
