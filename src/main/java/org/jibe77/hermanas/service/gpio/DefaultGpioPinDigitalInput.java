package org.jibe77.hermanas.service.gpio;

import com.pi4j.common.Metadata;
import com.pi4j.context.Context;
import com.pi4j.exception.InitializeException;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.binding.DigitalBinding;
import com.pi4j.io.gpio.digital.*;

public class DefaultGpioPinDigitalInput implements DigitalInput {

    @Override
    public DigitalState state() {
        // LOW (button released, output off) — never null. Returning null makes
        // the default Digital.isHigh()/isLow() helpers NPE the first time
        // anything queries the pin, which crashes the gpio-fake boot path used
        // by `mvn spring-boot:run -Dspring.profiles.active=gpio-fake` on dev
        // machines: DoorService.initDoorAccordingToSunTime() reads the limit
        // switches as soon as the bean is wired.
        return DigitalState.LOW;
    }

    @Override
    public DigitalInput addListener(DigitalStateChangeListener... listener) {
        // Return `this` so the fluent `.addListener(...)` chain in
        // UpButtonService / BottomButtonService doesn't NPE if the next call
        // is .addListener again — pi4j's real DigitalInput returns itself.
        return this;
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
