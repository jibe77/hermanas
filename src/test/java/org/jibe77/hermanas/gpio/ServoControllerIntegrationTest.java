package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import org.jibe77.hermanas.gpio.door.BottomButton;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.DoorController;
import org.jibe77.hermanas.gpio.door.ServoMotor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DoorController.class, BottomButton.class})
public class ServoControllerIntegrationTest {

    @Autowired
    DoorController controller;

    @MockBean
    ServoMotor servoMotor;

    @MockBean
    GpioControllerSingleton gpioControllerSingleton;

    @MockBean
    GpioController gpioController;

    @MockBean
    GpioPinDigitalInput gpioPinDigitalInput;

    Logger logger = LoggerFactory.getLogger(ServoControllerIntegrationTest.class);

    @Test
    public void testCloseDoor() throws DoorNotClosedCorrectlyException {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(gpioControllerSingleton.getController()).thenReturn(gpioController);
        Mockito.when(
                gpioController.provisionDigitalInputPin(
                        Mockito.any(Pin.class),
                        Mockito.any(PinPullResistance.class))
        ).thenReturn(gpioPinDigitalInput);
        Mockito.when(servoMotor.setPosition(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);
        controller.closeDoorWithBottormButtonManagement();
        Mockito.verify(
                servoMotor,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        // TODO : add more verifications.
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... finished !");
    }

    @Test
    public void testOpenDoor() {
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... started.");
        controller.openDoor();
        Mockito.verify(
                servoMotor,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... finished !");
    }


}
