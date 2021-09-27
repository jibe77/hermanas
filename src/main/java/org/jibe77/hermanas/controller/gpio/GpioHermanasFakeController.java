package org.jibe77.hermanas.controller.gpio;

import com.pi4j.common.Metadata;
import com.pi4j.context.Context;
import com.pi4j.exception.InitializeException;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.binding.DigitalBinding;
import com.pi4j.io.gpio.digital.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Controller
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-fake")
public class GpioHermanasFakeController implements GpioHermanasController {

    Logger logger = LoggerFactory.getLogger(GpioHermanasFakeController.class);

    private boolean cameraIsInitialised;

    @PostConstruct
    private void initialise() {
        logger.warn("Initialise fake GPIO.");
    }

    @PreDestroy
    private void tearDown() {
        logger.info("Shutdown fake GPIO.");
    }

    public void moveServo(int doorServoGpioAddress, int positionNumber) {
        //send the value to the motor.
        logger.info("Fake GPIO : moving servo motor on Pin address {} on gear position {}.",
                doorServoGpioAddress, positionNumber);
    }

    @Override
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQuality) throws IOException {
        logger.info("Fake GPIO : take picture ");
        if (!cameraIsInitialised) {
            throw new IllegalStateException("Fake GPIO : can't hasn't been initialised before taking picture !");
        }
    }

    public DigitalInput provisionInput(String id, String name, int gpioAddress) {
        logger.info("Fake GPIO : provision input pin on GPIO address {}.", gpioAddress);
        return new DefaultGpioPinDigitalInput();
    }

    @Override
    public DigitalOutput provisionOutput(int gpioAddress) {
        logger.info("Fake GPIO : provision output pin on GPIO address {}.", gpioAddress);
        return new DigitalOutput() {
            @Override
            public DigitalOutput state(DigitalState state) throws com.pi4j.io.exception.IOException {
                return null;
            }

            @Override
            public DigitalOutput pulse(int interval, TimeUnit unit, DigitalState state, Callable<Void> callback) throws com.pi4j.io.exception.IOException {
                return null;
            }

            @Override
            public Future<?> pulseAsync(int interval, TimeUnit unit, DigitalState state, Callable<Void> callback) {
                return null;
            }

            @Override
            public DigitalOutput blink(int delay, int duration, TimeUnit unit, DigitalState state, Callable<Void> callback) {
                return null;
            }

            @Override
            public Future<?> blinkAsync(int delay, int duration, TimeUnit unit, DigitalState state, Callable<Void> callback) {
                return null;
            }

            @Override
            public DigitalOutput on() throws com.pi4j.io.exception.IOException {
                return null;
            }

            @Override
            public DigitalOutput off() throws com.pi4j.io.exception.IOException {
                return null;
            }

            @Override
            public DigitalState state() {
                return null;
            }

            @Override
            public DigitalOutput addListener(DigitalStateChangeListener... listener) {
                return null;
            }

            @Override
            public DigitalOutput removeListener(DigitalStateChangeListener... listener) {
                return null;
            }

            @Override
            public DigitalOutputConfig config() {
                return null;
            }

            @Override
            public DigitalOutput name(String name) {
                return null;
            }

            @Override
            public DigitalOutput description(String description) {
                return null;
            }

            @Override
            public DigitalOutputProvider provider() {
                return null;
            }

            @Override
            public String id() {
                return null;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public Metadata metadata() {
                return null;
            }

            @Override
            public Object initialize(Context context) throws InitializeException {
                return null;
            }

            @Override
            public Object shutdown(Context context) throws ShutdownException {
                return null;
            }

            @Override
            public boolean isOn() {
                return false;
            }

            @Override
            public DigitalOutput bind(DigitalBinding... binding) {
                return null;
            }

            @Override
            public DigitalOutput unbind(DigitalBinding... binding) {
                return null;
            }
        };
    }
}
