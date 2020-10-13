package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.trigger.GpioTrigger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultGpioPinDigital implements GpioPinDigitalInput, GpioPinDigitalOutput {

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
}
