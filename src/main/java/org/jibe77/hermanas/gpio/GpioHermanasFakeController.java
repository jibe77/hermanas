package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.trigger.GpioTrigger;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    public void initCamera(int photoWidth, int photoHeight, String photoEncoding, int photoQuality, int photoDelay, int photoRotation) {
        logger.info(
            "Fake GPIO : init camera with photoWidth {} photoHeight {} photoEncoding {} photoQuality {} photoDelay {} photoRotation {}",
                photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay, photoRotation);
        cameraIsInitialised = true;
    }

    @Override
    public void takePicture(FilePictureCaptureHandler filePictureCaptureHandler) throws IOException {
        logger.info("Fake GPIO : take picture ");
        if (!cameraIsInitialised) {
            throw new IllegalStateException("Fake GPIO : can't hasn't been initialised before taking picture !");
        }
    }

    public GpioPinDigitalInput provisionInput(int gpioAddress) {
        logger.info("Fake GPIO : provision input pin on GPIO address {}.", gpioAddress);
        return new GpioPinDigitalInput() {

            @Override
            public GpioProvider getProvider() {
                return null;
            }

            @Override
            public Pin getPin() {
                return null;
            }

            @Override
            public void setName(String name) {
                // default implementation
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setTag(Object tag) {
                // default implementation
            }

            @Override
            public Object getTag() {
                return null;
            }

            @Override
            public void setProperty(String key, String value) {
                // default implementation
            }

            @Override
            public boolean hasProperty(String key) {
                return false;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return null;
            }

            @Override
            public Map<String, String> getProperties() {
                return null;
            }

            @Override
            public void removeProperty(String key) {
                // default implementation
            }

            @Override
            public void clearProperties() {
                // default implementation
            }

            @Override
            public void export(PinMode mode) {
                // default implementation
            }

            @Override
            public void export(PinMode mode, PinState defaultState) {
                // default implementation
            }

            @Override
            public void unexport() {
                // default implementation
            }

            @Override
            public boolean isExported() {
                return false;
            }

            @Override
            public void setMode(PinMode mode) {
                // default implementation
            }

            @Override
            public PinMode getMode() {
                return null;
            }

            @Override
            public boolean isMode(PinMode mode) {
                return false;
            }

            @Override
            public void setPullResistance(PinPullResistance resistance) {
                // default implementation
            }

            @Override
            public PinPullResistance getPullResistance() {
                return null;
            }

            @Override
            public boolean isPullResistance(PinPullResistance resistance) {
                return false;
            }

            @Override
            public Collection<GpioPinListener> getListeners() {
                return Collections.emptyList();
            }

            @Override
            public void addListener(GpioPinListener... listener) {
                // default implementation
            }

            @Override
            public void addListener(List<? extends GpioPinListener> listeners) {
                // default implementation
            }

            @Override
            public boolean hasListener(GpioPinListener... listener) {
                return false;
            }

            @Override
            public void removeListener(GpioPinListener... listener) {
                // default implementation
            }

            @Override
            public void removeListener(List<? extends GpioPinListener> listeners) {
                // default implementation
            }

            @Override
            public void removeAllListeners() {
                // default implementation
            }

            @Override
            public GpioPinShutdown getShutdownOptions() {
                return null;
            }

            @Override
            public void setShutdownOptions(GpioPinShutdown options) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance, PinMode mode) {
                // default implementation
            }

            @Override
            public Collection<GpioTrigger> getTriggers() {
                return Collections.emptyList();
            }

            @Override
            public void addTrigger(GpioTrigger... trigger) {
                // default implementation
            }

            @Override
            public void addTrigger(List<? extends GpioTrigger> triggers) {
                // default implementation
            }

            @Override
            public void removeTrigger(GpioTrigger... trigger) {
                // default implementation
            }

            @Override
            public void removeTrigger(List<? extends GpioTrigger> triggers) {
                // default implementation
            }

            @Override
            public void removeAllTriggers() {
                // default implementation
            }

            @Override
            public boolean isHigh() {
                return false;
            }

            @Override
            public boolean isLow() {
                return false;
            }

            @Override
            public PinState getState() {
                return null;
            }

            @Override
            public boolean isState(PinState state) {
                return false;
            }

            @Override
            public boolean hasDebounce(PinState state) {
                return false;
            }

            @Override
            public int getDebounce(PinState state) {
                return 0;
            }

            @Override
            public void setDebounce(int debounce) {
                // default implementation
            }

            @Override
            public void setDebounce(int debounce, PinState... state) {
                // default implementation
            }
        };
    }

    public void unprovisionPin(GpioPin gpioPin) {
        logger.info("Fake GPIO : unprovision pin.");
    }

    @Override
    public GpioPinDigitalOutput provisionOutput(int gpioAddress) {
        logger.info("Fake GPIO : provision output pin on GPIO address {}.", gpioAddress);
        return new GpioPinDigitalOutput() {
            @Override
            public GpioProvider getProvider() {
                return null;
            }

            @Override
            public Pin getPin() {
                return null;
            }

            @Override
            public void setName(String name) {
                // default implementation
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setTag(Object tag) {
                // default implementation
            }

            @Override
            public Object getTag() {
                return null;
            }

            @Override
            public void setProperty(String key, String value) {
                // default implementation
            }

            @Override
            public boolean hasProperty(String key) {
                return false;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return null;
            }

            @Override
            public Map<String, String> getProperties() {
                return null;
            }

            @Override
            public void removeProperty(String key) {
                // default implementation
            }

            @Override
            public void clearProperties() {
                // default implementation
            }

            @Override
            public void export(PinMode mode) {
                // default implementation
            }

            @Override
            public void export(PinMode mode, PinState defaultState) {
                // default implementation
            }

            @Override
            public void unexport() {
                // default implementation
            }

            @Override
            public boolean isExported() {
                return false;
            }

            @Override
            public void setMode(PinMode mode) {
                // default implementation
            }

            @Override
            public PinMode getMode() {
                return null;
            }

            @Override
            public boolean isMode(PinMode mode) {
                return false;
            }

            @Override
            public void setPullResistance(PinPullResistance resistance) {
                // default implementation
            }

            @Override
            public PinPullResistance getPullResistance() {
                return null;
            }

            @Override
            public boolean isPullResistance(PinPullResistance resistance) {
                return false;
            }

            @Override
            public Collection<GpioPinListener> getListeners() {
                return Collections.emptyList();
            }

            @Override
            public void addListener(GpioPinListener... listener) {
                // default implementation
            }

            @Override
            public void addListener(List<? extends GpioPinListener> listeners) {
                // default implementation
            }

            @Override
            public boolean hasListener(GpioPinListener... listener) {
                return false;
            }

            @Override
            public void removeListener(GpioPinListener... listener) {
                // default implementation
            }

            @Override
            public void removeListener(List<? extends GpioPinListener> listeners) {
                // default implementation
            }

            @Override
            public void removeAllListeners() {
                // default implementation
            }

            @Override
            public GpioPinShutdown getShutdownOptions() {
                return null;
            }

            @Override
            public void setShutdownOptions(GpioPinShutdown options) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance) {
                // default implementation
            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance, PinMode mode) {
                // default implementation
            }

            @Override
            public boolean isHigh() {
                return false;
            }

            @Override
            public boolean isLow() {
                return false;
            }

            @Override
            public PinState getState() {
                return null;
            }

            @Override
            public boolean isState(PinState state) {
                return false;
            }

            @Override
            public void high() {
                // default implementation
            }

            @Override
            public void low() {
                // default implementation
            }

            @Override
            public void toggle() {
                // default implementation
            }

            @Override
            public Future<?> blink(long delay) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, PinState blinkState) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, PinState blinkState, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, long duration) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, long duration, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, long duration, PinState blinkState) {
                return null;
            }

            @Override
            public Future<?> blink(long delay, long duration, PinState blinkState, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, Callable<Void> callback) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, Callable<Void> callback, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, boolean blocking) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, boolean blocking, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, boolean blocking, Callable<Void> callback) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, boolean blocking, Callable<Void> callback, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, Callable<Void> callback) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, Callable<Void> callback, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, boolean blocking) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, boolean blocking, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, boolean blocking, Callable<Void> callback) {
                return null;
            }

            @Override
            public Future<?> pulse(long duration, PinState pulseState, boolean blocking, Callable<Void> callback, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public void setState(PinState state) {
                // default implementation
            }

            @Override
            public void setState(boolean state) {
                // default implementation
            }
        };
    }

    @Override
    public void initSensor(int pinNumber) {
        logger.info("init sensor on gpio address {}.", pinNumber);
    }

    @Override
    public void sendStartSignal(int pinNumber) {
        logger.info("send start signal to sensor on pin number {}.", pinNumber);
    }
}
