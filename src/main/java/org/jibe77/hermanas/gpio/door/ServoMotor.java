package org.jibe77.hermanas.gpio.door;

import com.pi4j.wiringpi.*;
import org.jibe77.hermanas.gpio.GpioControllerSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class manipulates a servo motor.
 *
 * @author Fabio Hedayioglu
 * @author Ian Utting
 * @version 1.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServoMotor
{
    final
    GpioControllerSingleton gpioControllerSingleton;

    boolean bottomButtonPressed;

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    Logger logger = LoggerFactory.getLogger(ServoMotor.class);

    public ServoMotor(GpioControllerSingleton gpioControllerSingleton) {
        this.gpioControllerSingleton = gpioControllerSingleton;
    }

    public synchronized boolean setPosition(int positionNumber, int sleep) {
        if (positionNumber >= 5 && positionNumber <= 25)
        {
            bottomButtonPressed = false;
            //send the value to the motor.
            SoftPwm.softPwmWrite(doorServoGpioAddress, positionNumber);
            //give time to the motor to reach the position
            sleepMillisec(sleep);
            //stop sending orders to the motor.
            if (!bottomButtonPressed)
                stop();
            return bottomButtonPressed;
        } else {
            throw new IllegalArgumentException("Nothing done, positionNumber has to be between 5 and 25");
        }
    }

    /**
     * turn servo off
     */
    public void stop(){
        //zero tells the motor to turn itself off and wait for more instructions.
        SoftPwm.softPwmWrite(doorServoGpioAddress, 0);
    }

    public void stopAtBottom() {
        bottomButtonPressed = true;
        stop();
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
        }
    }
}
