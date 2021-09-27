package org.jibe77.hermanas.controller.gpio;

import com.pi4j.common.Metadata;
import com.pi4j.context.Context;
import com.pi4j.exception.InitializeException;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.binding.DigitalBinding;
import com.pi4j.io.gpio.digital.*;

public class DefaultGpioPinDigitalInput implements DigitalInput {

    @Override
    public DigitalState state() {
        return null;
    }

    @Override
    public DigitalInput addListener(DigitalStateChangeListener... listener) {
        return null;
    }

    @Override
    public DigitalInput removeListener(DigitalStateChangeListener... listener) {
        return null;
    }

    @Override
    public boolean isOn() {
        return false;
    }

    @Override
    public DigitalInput bind(DigitalBinding... binding) {
        return null;
    }

    @Override
    public DigitalInput unbind(DigitalBinding... binding) {
        return null;
    }

    @Override
    public DigitalInputConfig config() {
        return null;
    }

    @Override
    public DigitalInput name(String name) {
        return null;
    }

    @Override
    public DigitalInput description(String description) {
        return null;
    }

    @Override
    public DigitalInputProvider provider() {
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
}
