package org.jibe77.hermanas.controller.door.bottombutton;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class BottomButtonListenerTest {

    BottomButtonListener bottomButtonListener;

    BottomButtonController bottomButtonController = mock(BottomButtonController.class);

    ServoMotorController servoMotorController = mock(ServoMotorController.class);

    @BeforeEach
    public void setup() {
        bottomButtonListener = new BottomButtonListener(bottomButtonController, servoMotorController);
    }

    @Test
    void testBottomButtonIsPressed() {
        GpioPinDigitalStateChangeEvent event = mock(GpioPinDigitalStateChangeEvent.class);
        when(event.getState()).thenReturn(PinState.HIGH);

        bottomButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        verify(bottomButtonController, times(1)).setBottomButtonHasBeenPressed(true);
        verify(servoMotorController, times(1)).stop();
    }

    @Test
    void testBottomButtonIsUnpressed() {
        GpioPinDigitalStateChangeEvent event = mock(GpioPinDigitalStateChangeEvent.class);
        when(event.getState()).thenReturn(PinState.LOW);

        bottomButtonListener.handleGpioPinDigitalStateChangeEvent(event);

        verify(bottomButtonController, times(0)).setBottomButtonHasBeenPressed(anyBoolean());
        verify(servoMotorController, times(0)).stop();
    }
}
