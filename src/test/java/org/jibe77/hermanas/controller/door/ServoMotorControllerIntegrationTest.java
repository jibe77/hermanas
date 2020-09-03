package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.gpio.GpioHermanasFakeController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ServoMotorController.class})
class ServoMotorControllerIntegrationTest {

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
        servoMotorController.setPosition(14, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }

    @Test
    void testServoMotorCounterClockwise() {
        servoMotorController.setPosition(15, 100);
        assertTrue(true, "The door is supposed to move right now !");
    }

    @Test
    void test() {
        File f1 = new File("test test&t.wav");
        File f2 = new File("c'est un test.wav");
        File f3 = new File("clkj ' & &t.wav");
        List<File> filesList = new ArrayList<File>();
        filesList.add(f1);
        filesList.add(f2);
        filesList.add(f3);
        System.out.println(filesList.stream()
                .map(f -> f.getAbsolutePath())
                .map(p -> p.replaceAll(" ", "\\\\ "))
                .map(p -> p.replaceAll("&", "\\\\&"))
                .map(p -> p.replaceAll("'", "\\\\'"))
                .collect(Collectors.joining(" ")));
    }

}
