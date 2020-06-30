package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import org.jibe77.hermanas.gpio.sensor.DHT22;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.caprica.picam.Camera;
import uk.co.caprica.picam.CameraConfiguration;
import uk.co.caprica.picam.CaptureFailedException;
import uk.co.caprica.picam.FilePictureCaptureHandler;
import uk.co.caprica.picam.enums.Encoding;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiController implements GpioHermanasController {

    private GpioController gpio;

    private CameraConfiguration config;

    Logger logger = LoggerFactory.getLogger(GpioHermanasRpiController.class);

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    @Value("${door.servo.gpio.range}")
    private int doorSettingRange;

    @PostConstruct
    private void initialiseGpioPins() {
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

    @Override
    public void initCamera(int photoWidth, int photoHeight, String photoEncoding, int photoQuality, int photoDelay) {
        logger.info("init camera config with width {} height {} encoding {} quality {} and delay {}.",
                photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay);
        config = cameraConfiguration()
                .width(photoWidth)
                .height(photoHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(photoQuality)
                .delay(photoDelay);
    }

    @Override
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler) throws IOException {
        try (Camera camera = new Camera(config)){
            camera.takePicture(filePictureCaptureHandler);
        } catch (CaptureFailedException e) {
            throw new IOException("Can't capture a picture.", e);
        } catch (Exception e) {
            throw new IOException(e);
        }
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

    public GpioPinDigitalInput provisionInput(int gpioAddress) {
        return gpio.provisionDigitalInputPin(
                RaspiPin.getPinByAddress(gpioAddress), PinPullResistance.PULL_DOWN);
    }

    @Override
    public GpioPinDigitalOutput provisionOutput(int gpioAddress) {
        GpioPinDigitalOutput gpioPinDigitalOutput = gpio.provisionDigitalOutputPin(
                RaspiPin.getPinByAddress(gpioAddress));
        gpioPinDigitalOutput.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        return gpioPinDigitalOutput;
    }

    public void unprovisionPin(GpioPin bottomButton) {
        gpio.unprovisionPin(bottomButton);
    }

    @Override
    public void initSensor(int pinNumber) {
        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }

    @Override
    public void sendStartSignal(int pinNumber) {
        // Send start signal.
        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.LOW);
        Gpio.delay(10);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }

    @Override
    public void waitForResponseSignal(int pinNumber, boolean keepRunning) {
        Gpio.pinMode(pinNumber, Gpio.INPUT);
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
        }
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.LOW) {
        }
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
        }
    }

    @Override
    public void close(int pinNumber) {
        // Set pin high for end of transmission.
        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }

    @Override
    public byte[] fetchData(int pinNumber, boolean keepRunning, long startTime) {
        byte[] data = new byte[5];
        for (int i = 0; i < 40; i++) {
            while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.LOW) {
            }
            startTime = System.nanoTime();
            while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
            }
            long timeHight = System.nanoTime() - startTime;
            data[i / 8] <<= 1;
            if ( timeHight > DHT22.LONGEST_ZERO) {
                data[i / 8] |= 1;
            }
        }
        return data;
    }
}
