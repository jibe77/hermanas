package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.trigger.GpioTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component()
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-fake")
public class GpioHermanasFakeController implements GpioHermanasController {

    Logger logger = LoggerFactory.getLogger(GpioHermanasFakeController.class);

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

    public GpioPinDigitalInput provisionButton(int gpioAddress) {
        logger.info("Fake GPIO : provision button on GPIO address {}.", gpioAddress);
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

    public void unprovisionButton(GpioPinDigitalInput bottomButton) {
        logger.info("Fake GPIO : unprovision button.");
    }
}
