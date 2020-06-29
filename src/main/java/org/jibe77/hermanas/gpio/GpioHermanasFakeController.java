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
    public void initCamera(int photoWidth, int photoHeight, String photoEncoding, int photoQuality, int photoDelay) {
        logger.info(
            "Fake GPIO : init camera with photoWidth {} photoHeight {} photoEncoding {} photoQuality {} photoDelay {}",
                photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay);
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

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setTag(Object tag) {

            }

            @Override
            public Object getTag() {
                return null;
            }

            @Override
            public void setProperty(String key, String value) {

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

            }

            @Override
            public void clearProperties() {

            }

            @Override
            public void export(PinMode mode) {

            }

            @Override
            public void export(PinMode mode, PinState defaultState) {

            }

            @Override
            public void unexport() {

            }

            @Override
            public boolean isExported() {
                return false;
            }

            @Override
            public void setMode(PinMode mode) {

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
                return null;
            }

            @Override
            public void addListener(GpioPinListener... listener) {

            }

            @Override
            public void addListener(List<? extends GpioPinListener> listeners) {

            }

            @Override
            public boolean hasListener(GpioPinListener... listener) {
                return false;
            }

            @Override
            public void removeListener(GpioPinListener... listener) {

            }

            @Override
            public void removeListener(List<? extends GpioPinListener> listeners) {

            }

            @Override
            public void removeAllListeners() {

            }

            @Override
            public GpioPinShutdown getShutdownOptions() {
                return null;
            }

            @Override
            public void setShutdownOptions(GpioPinShutdown options) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance, PinMode mode) {

            }

            @Override
            public Collection<GpioTrigger> getTriggers() {
                return null;
            }

            @Override
            public void addTrigger(GpioTrigger... trigger) {

            }

            @Override
            public void addTrigger(List<? extends GpioTrigger> triggers) {

            }

            @Override
            public void removeTrigger(GpioTrigger... trigger) {

            }

            @Override
            public void removeTrigger(List<? extends GpioTrigger> triggers) {

            }

            @Override
            public void removeAllTriggers() {

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

            }

            @Override
            public void setDebounce(int debounce, PinState... state) {

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

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setTag(Object tag) {

            }

            @Override
            public Object getTag() {
                return null;
            }

            @Override
            public void setProperty(String key, String value) {

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

            }

            @Override
            public void clearProperties() {

            }

            @Override
            public void export(PinMode mode) {

            }

            @Override
            public void export(PinMode mode, PinState defaultState) {

            }

            @Override
            public void unexport() {

            }

            @Override
            public boolean isExported() {
                return false;
            }

            @Override
            public void setMode(PinMode mode) {

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
                return null;
            }

            @Override
            public void addListener(GpioPinListener... listener) {

            }

            @Override
            public void addListener(List<? extends GpioPinListener> listeners) {

            }

            @Override
            public boolean hasListener(GpioPinListener... listener) {
                return false;
            }

            @Override
            public void removeListener(GpioPinListener... listener) {

            }

            @Override
            public void removeListener(List<? extends GpioPinListener> listeners) {

            }

            @Override
            public void removeAllListeners() {

            }

            @Override
            public GpioPinShutdown getShutdownOptions() {
                return null;
            }

            @Override
            public void setShutdownOptions(GpioPinShutdown options) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance) {

            }

            @Override
            public void setShutdownOptions(Boolean unexport, PinState state, PinPullResistance resistance, PinMode mode) {

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

            }

            @Override
            public void low() {

            }

            @Override
            public void toggle() {

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

            }

            @Override
            public void setState(boolean state) {

            }
        };
    }


}
