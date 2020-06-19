package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component()
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiController implements GpioHermanasController {

    private GpioController gpio;

    Logger logger = LoggerFactory.getLogger(GpioHermanasRpiController.class);

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    @Value("${door.servo.gpio.range}")
    private int doorSettingRange;

    @PostConstruct
    private void initialise() {
        logger.info("Initialise GPIO ...");
        try {
            gpio = GpioFactory.getInstance();

            /*
             * Before interacting with Pi4J, you must first create a new GPIO
             *  controller instance. The GpioFactory includes a createInstance
             * method to create the GPIO controller. Your project should only
             * instantiate a single GPIO controller instance and that instance
             * should be shared across your project.
             */
            Gpio.wiringPiSetup();

            //Set the PinNumber pin to be a PWM pin, with values changing from 0 to 250
            //this will give enough resolution to the servo motor
            int returnValue = SoftPwm.softPwmCreate(
                    doorServoGpioAddress,
                    0,
                    doorSettingRange);
            if (returnValue != 0) {
                logger.warn("The door setting doesn't seem to initialise correctly, return value : {}", returnValue);
            } else {
                logger.info("The door servomotor has been initialised.");
            }
        } catch (UnsatisfiedLinkError e) {
            logger.error("Can't find wiringpi, is it installed on your machine ?", e);
        }
        logger.info("... initialisation done.");
    }

    @PreDestroy
    private void tearDown() {
        logger.info("Shutdown gpio instance.");
        gpio.shutdown();
    }

    public void moveServo(int doorServoGpioAddress, int positionNumber) {
        //send the value to the motor.
        SoftPwm.softPwmWrite(doorServoGpioAddress, positionNumber);
    }

    public GpioPinDigitalInput provisionButton(int gpioAddress) {
        return gpio.provisionDigitalInputPin(
                RaspiPin.getPinByAddress(gpioAddress), PinPullResistance.PULL_DOWN);
    }

    public void unprovisionButton(GpioPinDigitalInput bottomButton) {
        gpio.unprovisionPin(bottomButton);
    }
}
