package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.gpio.DefaultGpioPwm;
import org.jibe77.hermanas.controller.gpio.GpioHermanasFakeController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ServoMotorController.class})
class ServoMotorControllerTest {

    @Autowired
    ServoMotorController servoMotorController;

    @MockBean
    GpioHermanasFakeController gpioHermanasController;

    @Test
    void testServoMotorErrorPosition() {
        try {
            servoMotorController.setPosition(0, 100);
            assertTrue(
                    false,
                    "This code should not be reached because positionNumber argument is between 5 and 25.");
        } catch (IllegalArgumentException e) {
            assertTrue(true, "This exception is expected.");
        }
    }

    @Test
    void testServoMotorClockwise() {
        Mockito.when(
                gpioHermanasController.provisionPwm(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt())).thenReturn(new DefaultGpioPwm());
        servoMotorController.provisionPwm();
        servoMotorController.setPosition(14, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }

    @Test
    void testServoMotorCounterClockwise() {
        Mockito.when(
                gpioHermanasController.provisionPwm(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt())).thenReturn(new DefaultGpioPwm());
        servoMotorController.provisionPwm();
        servoMotorController.setPosition(15, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }
}
