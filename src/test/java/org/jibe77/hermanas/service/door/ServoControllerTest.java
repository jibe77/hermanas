package org.jibe77.hermanas.service.door;

import com.pi4j.io.gpio.digital.DigitalInput;
import org.jibe77.hermanas.service.door.bottombutton.BottomButtonService;
import org.jibe77.hermanas.service.door.upbutton.UpButtonService;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.service.door.servo.ServoMotorService;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.websocket.NotificationController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DoorService.class})
class ServoControllerTest {

    @Autowired
    DoorService doorService;

    @MockBean
    ServoMotorService servoMotorService;

    @MockBean
    GpioHermanasService gpioHermanasService;

    @MockBean
    BottomButtonService bottomButtonService;

    @MockBean
    UpButtonService upButtonService;

    @MockBean
    SunTimeManager sunTimeManager;

    @MockBean
    DigitalInput gpioPinDigitalInput;

    @MockBean
    NotificationController notificationController;

    @MockBean
    ConfigService configService;

    Logger logger = LoggerFactory.getLogger(ServoControllerTest.class);

    @Test
    void testCloseDoor() {
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... started.");
        Mockito.when(
                gpioHermanasService.provisionInput(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())
        ).thenReturn(gpioPinDigitalInput);
        doorService.closeDoor(true, false);
        Mockito.verify(
                servoMotorService,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        logger.info("<--Pi4J--> GPIO Control CloseDoor ... finished !");
    }

    @Test
    void testOpenDoor() {
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... started.");
        doorService.openDoor(false, false);
        Mockito.verify(
                servoMotorService,
                Mockito.times(1)
        ).setPosition(
                Mockito.anyInt(),
                Mockito.anyInt());
        logger.info("<--Pi4J--> GPIO Control OpenDoor ... finished !");
    }


}
