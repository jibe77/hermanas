package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import uk.co.caprica.picam.*;
import uk.co.caprica.picam.CameraConfiguration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiController implements GpioHermanasController {

    private GpioController gpio;

    private CameraConfiguration highQualityConfig;

    private CameraConfiguration regularQualityconfig;

    Logger logger = LoggerFactory.getLogger(GpioHermanasRpiController.class);

    @Value("${door.servo.gpio.address}")
    private int doorServoGpioAddress;

    @Value("${door.servo.gpio.range}")
    private int doorSettingRange;

    @Value("${camera.regular.delay}")
    private int photoRegularDelay;

    @Value("${camera.high.delay}")
    private int photoHighDelay;

    @Value("${camera.picam.jni.implementation}")
    private String picamJniImplementation;

    public GpioHermanasRpiController(@Qualifier("CameraHighQualityConfig") CameraConfiguration highQualityConfig,
                                     @Qualifier("CameraRegularQualityConfig") CameraConfiguration regularQualityconfig) {
        this.highQualityConfig = highQualityConfig;
        this.regularQualityconfig = regularQualityconfig;
    }

    @PostConstruct
    private void initialiseGpioPins() {
        logger.info("Initialise GPIO ...");
        try {
            logger.info("Extract the bundled picam native library to a temporary file and load it.");
            System.load(picamJniImplementation);

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
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        CompletableFuture<Void> future= takePictureAsync(filePictureCaptureHandler, highQuality);
        try {
            future.get( 10, SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Async
    public CompletableFuture<Void> takePictureAsync(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        try (Camera camera = new Camera(highQuality ? highQualityConfig : regularQualityconfig)){
            camera.takePicture(filePictureCaptureHandler, highQuality ? photoHighDelay : photoRegularDelay);
        } catch (CaptureFailedException e) {
            throw new IOException("Can't capture a picture.", e);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return CompletableFuture.completedFuture(null);
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
}
