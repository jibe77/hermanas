package org.jibe77.hermanas.gpio;

import org.jibe77.hermanas.gpio.door.ServoMotorController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.Assert;

@SpringBootTest(classes = {ServoMotorController.class})
public class ServoMotorControllerIntegrationTest {

    @Autowired
    ServoMotorController servoMotorController;

    @MockBean
    GpioControllerSingleton gpioControllerSingleton;

    @Test
    public void testServoMotorErrorPosition() {
        try {
            servoMotorController.setPosition(0, 100);
            Assert.isTrue(
                    false,
                    "This code should not be reached because positionNumber argument is between 5 and 25.");
        } catch (IllegalArgumentException e) {
            Assert.isTrue(true, "This exception is expected.");
        }
    }

    @Test
    public void testServoMotorClockwise() {
        servoMotorController.setPosition(14, 100);
        Assert.isTrue(true, "The door is supposed to move right now !");
    }

    @Test
    public void testServoMotorCounterClockwise() {
        servoMotorController.setPosition(15, 100);
        Assert.isTrue(true, "The door is supposed to move right now !");
    }

}
