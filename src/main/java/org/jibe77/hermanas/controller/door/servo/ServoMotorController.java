package org.jibe77.hermanas.controller.door.servo;

import com.pi4j.io.pwm.Pwm;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * This class manipulates a servo motor.
 *
 * @author Fabio Hedayioglu
 * @author Ian Utting
 * @version 1.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServoMotorController
{
    final
    GpioHermanasController gpioHermanasController;

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    private Pwm pwm;

    // clockwise positions
    private static final int SERVO_CLOSING_MIN_POSITION = 5;
    private static final int SERVO_CLOSING_MAX_POSITION = 14;
    // counter-clockwise positions
    private static final int SERVO_OPENING_MIN_POSITION = 15;
    private static final int SERVO_OPENING_MAX_POSITION = 25;


    Logger logger = LoggerFactory.getLogger(ServoMotorController.class);

    public ServoMotorController(GpioHermanasController gpioHermanasController) {
        this.gpioHermanasController = gpioHermanasController;
    }

    @PostConstruct
    public void provisionPwm() {
        logger.info("provision pwm servo motor on gpio instance.");
        if (pwm == null) {
            pwm = gpioHermanasController.provisionPwm("servo", "Servo", doorServoGpioAddress);
        }
    }

    public synchronized void setPosition(int positionNumber, int sleep) {
        // if the motor is moving clockwise, it means the door is closing
        if ((positionNumber >= SERVO_CLOSING_MIN_POSITION && positionNumber <= SERVO_CLOSING_MAX_POSITION)
                || (positionNumber >= SERVO_OPENING_MIN_POSITION && positionNumber <= SERVO_OPENING_MAX_POSITION)) {
            moveServo(positionNumber);
            //give time to the motor to reach the position
            sleepMillisec(sleep);
            //stop sending orders to the motor.
            stop();
        } else {
            throw new IllegalArgumentException("Nothing done, positionNumber has to be between " +
                    SERVO_CLOSING_MIN_POSITION + " and " + SERVO_OPENING_MAX_POSITION);
        }
    }

    /**
     * turn servo off
     */
    public void stop(){
        //zero tells the motor to turn itself off and wait for more instructions.
        pwm.off();
    }

    private void moveServo(int positionNumber) {
        //send the value to the motor.
        pwm.on(positionNumber);
    }

    /**
     * Wait for a number of milliseconds
     * @param millisec the number of milliseconds to wait.
     */
    private void sleepMillisec(int millisec){
        try
        {
            Thread.sleep(millisec);
        }
        catch ( InterruptedException e)
        {
            logger.error("Sleep interrupted:", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }
}
