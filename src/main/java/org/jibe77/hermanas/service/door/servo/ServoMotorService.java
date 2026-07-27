package org.jibe77.hermanas.service.door.servo;

import com.pi4j.io.pwm.Pwm;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * This class manipulates a servo motor.
 *
 * @author Fabio Hedayioglu
 * @author Ian Utting
 * @version 1.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServoMotorService
{
    final
    GpioHermanasService gpioHermanasService;

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    @Value("${door.servo.gpio.range}")
    private int doorSettingRange;

    private Pwm pwm;

    /**
     * Last frequency used by {@link #moveServo}; needed to issue a deterministic
     * zero-duty-cycle pulse train in {@link #stop()}. Volatile so the end-stop button
     * listener (Pi4j thread) sees the value written by the door command thread.
     */
    private volatile int lastFrequency;

    /**
     * Set by {@link #stop()} so that a thread currently sleeping inside
     * {@link #setPosition} wakes up immediately instead of letting the motor coast
     * to the end of its planned duration after the limit switch has already stopped it.
     */
    private volatile boolean stopRequested;

    // clockwise positions
    private static final int SERVO_CLOSING_MIN_POSITION = 5;
    private static final int SERVO_CLOSING_MAX_POSITION = 14;
    // counter-clockwise positions
    private static final int SERVO_OPENING_MIN_POSITION = 15;
    private static final int SERVO_OPENING_MAX_POSITION = 25;


    private static final Logger logger = LoggerFactory.getLogger(ServoMotorService.class);

    public ServoMotorService(GpioHermanasService gpioHermanasService) {
        this.gpioHermanasService = gpioHermanasService;
    }

    @PostConstruct
    public void provisionPwm() {
        logger.info("provision pwm servo motor on gpio instance.");
        if (pwm == null) {
            pwm = gpioHermanasService.provisionPwm("servo", "Servo", doorServoGpioAddress);
        }
    }

    public synchronized void setPosition(int positionNumber, int sleep) {
        // if the motor is moving clockwise, it means the door is closing
        if ((positionNumber >= SERVO_CLOSING_MIN_POSITION && positionNumber <= SERVO_CLOSING_MAX_POSITION)
                || (positionNumber >= SERVO_OPENING_MIN_POSITION && positionNumber <= SERVO_OPENING_MAX_POSITION)) {
            stopRequested = false;
            moveServo(positionNumber, doorSettingRange);
            //give time to the motor to reach the position (or to the limit switch to trip)
            sleepUntilStopOrTimeout(sleep);
            // stop sending orders to the motor.
            stop();
        } else {
            throw new IllegalArgumentException("Nothing done, positionNumber has to be between " +
                    SERVO_CLOSING_MIN_POSITION + " and " + SERVO_OPENING_MAX_POSITION);
        }
    }

    /**
     * Turn the servo off as deterministically as possible.
     *
     * <p>{@code Pwm.off()} alone is not always sufficient on the Pi4j 2.4 / pigpio
     * software-PWM stack: the pin can be left at the last commanded duty cycle, which
     * keeps the servo receiving valid pulses and moving. Sending an explicit zero-
     * duty-cycle pulse train first forces the signal low before disabling PWM
     * generation entirely. This is the call path triggered by the bottom / up
     * end-stop button listeners.</p>
     */
    public void stop() {
        stopRequested = true;
        logger.info("servomotor stop requested (forcing duty cycle to 0).");
        if (lastFrequency > 0) {
            // explicit zero-duty pulse train to release the servo deterministically.
            pwm.on(0, lastFrequency);
        }
        pwm.off();
    }

    public void moveServo(int dutyCycle, int frequency) {
        //send the value to the motor.
        lastFrequency = frequency;
        pwm.on(dutyCycle, frequency);
    }

    /**
     * Sleep up to {@code millisec} ms, returning early as soon as {@link #stopRequested}
     * becomes true. Polls every 50 ms which is well below servo response time and far
     * above scheduler granularity.
     */
    private void sleepUntilStopOrTimeout(int millisec) {
        long deadline = System.currentTimeMillis() + millisec;
        try {
            while (!stopRequested && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            logger.error("Sleep interrupted:", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait for a number of milliseconds. Kept public for callers that need a plain
     * blocking pause unrelated to a motor move (e.g. {@code DoorService}).
     * @param millisec the number of milliseconds to wait.
     */
    public void sleepMillisec(int millisec){
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
