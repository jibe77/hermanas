package org.jibe77.hermanas.controller.door;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DoorController.class})
class ServoControllerTest {

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

    Logger logger = LoggerFactory.getLogger(ServoControllerTest.class);

    @Test
    void testCloseDoor() throws DoorNotClosedCorrectlyException {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(
                gpioHermanasController.provisionInput(
                        Mockito.anyInt())
        ).thenReturn(gpioPinDigitalInput);
        controller.closeDoorWithBottormButtonManagement(false);
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
        controller.openDoor(false, false);
        Mockito.verify(
                servoMotorController,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... finished !");
    }


}
