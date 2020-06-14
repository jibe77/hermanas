package org.jibe77.hermanas.gpio;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.ServoController;
import org.jibe77.hermanas.gpio.door.ServoMotor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {ServoController.class})
public class ServoControllerIntegrationTest {

    @Autowired
    ServoController controller;

    @MockBean
    ServoMotor servoMotor;

    Logger logger = LoggerFactory.getLogger(ServoControllerIntegrationTest.class);

    @Test
    public void testCloseDoor() throws DoorNotClosedCorrectlyException {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        controller.closeDoor();
        Mockito.verify(
                servoMotor,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
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
