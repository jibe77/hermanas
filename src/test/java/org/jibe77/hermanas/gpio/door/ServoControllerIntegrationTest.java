package org.jibe77.hermanas.gpio.door;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.jibe77.hermanas.gpio.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.gpio.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DoorController.class, BottomButtonController.class})
class ServoControllerIntegrationTest {

    @Autowired
    DoorController controller;

    @MockBean
    ServoMotorController servoMotorController;

    @MockBean
    GpioHermanasController gpioHermanasController;

    @MockBean
    GpioController gpioController;

    @MockBean
    GpioPinDigitalInput gpioPinDigitalInput;

    @MockBean
    BottomButtonController bottomButtonController;

    Logger logger = LoggerFactory.getLogger(ServoControllerIntegrationTest.class);

    @Test
    void testCloseDoor() throws DoorNotClosedCorrectlyException {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(
                gpioHermanasController.provisionInput(
                        Mockito.anyInt())
        ).thenReturn(gpioPinDigitalInput);
        Mockito.when(bottomButtonController.isBottomButtonHasBeenPressed()).thenReturn(true);
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
    void testOpenDoor() {
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
