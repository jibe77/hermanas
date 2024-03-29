package org.jibe77.hermanas.controller.door;

import com.pi4j.io.gpio.digital.DigitalInput;
import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.websocket.NotificationController;
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
    BottomButtonController bottomButtonController;

    @MockBean
    UpButtonController upButtonController;

    @MockBean
    SunTimeManager sunTimeManager;

    @MockBean
    DigitalInput gpioPinDigitalInput;

    @MockBean
    NotificationController notificationController;

    Logger logger = LoggerFactory.getLogger(ServoControllerTest.class);

    @Test
    void testCloseDoor() {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(
                gpioHermanasController.provisionInput(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())
        ).thenReturn(gpioPinDigitalInput);
        controller.closeDoor(true, false);
        Mockito.verify(
                servoMotorController,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
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
