package org.jibe77.hermanas.gpio;

import org.jibe77.hermanas.gpio.door.ServoMotor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest(classes = {ServoMotor.class})
public class ServoMotorIntegrationTest {

    @Autowired
    ServoMotor servoMotor;

    @Test
    public void testServoMotorErrorPosition() {
        try {
            servoMotor.setPosition(0, 100);
            Assert.isTrue(
                    false,
                    "This code should not be reached because positionNumber argument is between 5 and 25.");
        } catch (IllegalArgumentException e) {
            Assert.isTrue(true, "This exception is expected.");
        }
    }

    @Test
    public void testServoMotorClockwise() {
        servoMotor.setPosition(14, 100);
        Assert.isTrue(true, "The door is supposed to move right now !");
    }

    @Test
    public void testServoMotorCounterClockwise() {
        servoMotor.setPosition(15, 100);
        Assert.isTrue(true, "The door is supposed to move right now !");
    }

}
