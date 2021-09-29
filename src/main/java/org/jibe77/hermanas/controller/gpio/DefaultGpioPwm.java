package org.jibe77.hermanas.controller.gpio;

import com.pi4j.common.Metadata;
import com.pi4j.context.Context;
import com.pi4j.exception.InitializeException;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmPreset;
import com.pi4j.io.pwm.PwmProvider;

import java.util.Map;

public class DefaultGpioPwm implements Pwm {
    @Override
    public boolean isOn() {
        return false;
    }

    @Override
    public Pwm on() throws IOException {
        return null;
    }

    @Override
    public Pwm off() throws IOException {
        return null;
    }

    @Override
    public float getDutyCycle() throws IOException {
        return 0;
    }

    @Override
    public void setDutyCycle(Number dutyCycle) throws IOException {

    }

    @Override
    public int getFrequency() throws IOException {
        return 0;
    }

    @Override
    public int getActualFrequency() throws IOException {
        return 0;
    }

    @Override
    public void setFrequency(int frequency) throws IOException {

    }

    @Override
    public Map<String, PwmPreset> getPresets() {
        return null;
    }

    @Override
    public PwmPreset getPreset(String name) {
        return null;
    }

    @Override
    public PwmPreset deletePreset(String name) {
        return null;
    }

    @Override
    public Pwm addPreset(PwmPreset preset) {
        return null;
    }

    @Override
    public Pwm applyPreset(String name) throws IOException {
        return null;
    }

    @Override
    public PwmConfig config() {
        return null;
    }

    @Override
    public Pwm name(String name) {
        return null;
    }

    @Override
    public Pwm description(String description) {
        return null;
    }

    @Override
    public PwmProvider provider() {
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
