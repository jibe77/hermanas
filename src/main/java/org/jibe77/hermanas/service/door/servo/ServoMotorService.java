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
     * Coupe le servo.
     *
     * <p><b>Ne pas réintroduire le {@code pwm.on(0, lastFrequency)} qui précédait
     * l'appel à {@code off()}.</b> Il datait de pi4j 2.4 / pigpio, où le PWM était
     * <em>logiciel</em> : {@code off()} seul pouvait laisser la broche au dernier
     * rapport cyclique commandé, et une trame à zéro forçait la ligne bas.</p>
     *
     * <p>Sur pi4j 4.x avec le plugin FFM, le PWM est <em>matériel</em> et piloté par
     * sysfs ({@code /sys/class/pwm/pwmchipN/pwmM}). Là, {@code on(0, …)} <b>réactive</b>
     * le canal : le couple on/off régénérait le signal au lieu de le couper, et le
     * servo continuait de tourner malgré le fin de course — d'où les
     * {@code PWM is already disabled} en rafale dans les logs, chaque nouvel appui du
     * bouton relançant un arrêt sans effet.</p>
     *
     * <p>Chemin d'appel critique : les écouteurs des boutons de fin de course haut et
     * bas. Un servo qui ne s'arrête pas force contre sa butée, chauffe et consomme.</p>
     */
    public void stop() {
        stopRequested = true;
        logger.info("servomotor stop requested.");
        pwm.off();
    }

    public void moveServo(int dutyCycle, int frequency) {
        //send the value to the motor.
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
