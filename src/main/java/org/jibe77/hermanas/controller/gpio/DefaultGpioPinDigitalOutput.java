package org.jibe77.hermanas.controller.gpio;

import com.pi4j.common.Metadata;
import com.pi4j.context.Context;
import com.pi4j.exception.InitializeException;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.binding.DigitalBinding;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.gpio.digital.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultGpioPinDigitalOutput implements DigitalOutput {

    @Override
    public DigitalOutput state(DigitalState state) throws IOException {
        return null;
    }

    @Override
    public DigitalOutput pulse(int interval, TimeUnit unit, DigitalState state, Callable<Void> callback) throws IOException {
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
    public DigitalOutput on() throws IOException {
        return null;
    }

    @Override
    public DigitalOutput off() throws IOException {
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
}
