package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.gpio.door.BottomButtonController;
import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.DoorController;
import org.jibe77.hermanas.gpio.door.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DoorController.class, BottomButtonController.class})
public class ServoControllerIntegrationTest {

    @Autowired
    DoorController controller;

    @MockBean
    ServoMotorController servoMotorController;

    @MockBean
    GpioControllerSingleton gpioControllerSingleton;

    @MockBean
    GpioController gpioController;

    @MockBean
    GpioPinDigitalInput gpioPinDigitalInput;

    @MockBean
    BottomButtonController bottomButtonController;

    Logger logger = LoggerFactory.getLogger(ServoControllerIntegrationTest.class);

    @Test
    public void testCloseDoor() throws DoorNotClosedCorrectlyException {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(
                gpioControllerSingleton.provisionButton(
                        Mockito.anyInt())
        ).thenReturn(gpioPinDigitalInput);
        Mockito.when(bottomButtonController.isBottomButtonPressed()).thenReturn(true);
        controller.closeDoorWithBottormButtonManagement();
        Mockito.verify(
                servoMotorController,
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
                servoMotorController,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... finished !");
    }


}
