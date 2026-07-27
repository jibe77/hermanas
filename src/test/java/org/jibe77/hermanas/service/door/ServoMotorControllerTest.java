package org.jibe77.hermanas.service.door;

import org.jibe77.hermanas.service.gpio.DefaultGpioPwm;
import org.jibe77.hermanas.service.gpio.GpioHermanasFakeService;
import org.jibe77.hermanas.service.door.servo.ServoMotorService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ServoMotorService.class})
class ServoMotorControllerTest {

    @Autowired
    ServoMotorService servoMotorService;

    @MockitoBean
    GpioHermanasFakeService gpioHermanasService;

    @Test
    void testServoMotorErrorPosition() {
        try {
            servoMotorService.setPosition(0, 100);
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
                gpioHermanasService.provisionPwm(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt())).thenReturn(new DefaultGpioPwm());
        servoMotorService.provisionPwm();
        servoMotorService.setPosition(14, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }

    @Test
    void testServoMotorCounterClockwise() {
        Mockito.when(
                gpioHermanasService.provisionPwm(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt())).thenReturn(new DefaultGpioPwm());
        servoMotorService.provisionPwm();
        servoMotorService.setPosition(15, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }
}
