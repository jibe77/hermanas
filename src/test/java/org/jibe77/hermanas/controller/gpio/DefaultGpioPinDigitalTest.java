package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.GpioPinShutdown;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.trigger.GpioTrigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGpioPinDigitalTest {

    public static final String TEST = "test";

    @Test
    void testDefaultImpl() {
        DefaultGpioPinDigital defaultGpioPinDigital = new DefaultGpioPinDigital();

        assertNull(defaultGpioPinDigital.getProvider());
        assertNull(defaultGpioPinDigital.getPin());
        defaultGpioPinDigital.setName(TEST);
        assertNull(defaultGpioPinDigital.getName());
        defaultGpioPinDigital.setTag(TEST);
        assertNull(defaultGpioPinDigital.getTag());
        defaultGpioPinDigital.setProperty(TEST, TEST);
        assertNull(defaultGpioPinDigital.getProperty(TEST));
        assertFalse(defaultGpioPinDigital.hasProperty(TEST));
        defaultGpioPinDigital.removeProperty(TEST);
        assertNull(defaultGpioPinDigital.getProperty(TEST, TEST));
        assertNull(defaultGpioPinDigital.getProperties());
        defaultGpioPinDigital.clearProperties();
        assertNull(defaultGpioPinDigital.getProperties());
        defaultGpioPinDigital.export(PinMode.DIGITAL_INPUT);
        defaultGpioPinDigital.export(PinMode.DIGITAL_INPUT, PinState.HIGH);
        defaultGpioPinDigital.unexport();
        assertFalse(defaultGpioPinDigital.isExported());
        defaultGpioPinDigital.setMode(PinMode.DIGITAL_INPUT);
        assertNull(defaultGpioPinDigital.getMode());
        assertFalse(defaultGpioPinDigital.isMode(PinMode.DIGITAL_INPUT));
        defaultGpioPinDigital.setPullResistance(PinPullResistance.PULL_DOWN);
        assertNull(defaultGpioPinDigital.getPullResistance());
        assertFalse(defaultGpioPinDigital.isPullResistance(PinPullResistance.PULL_UP));
        defaultGpioPinDigital.addListener(mock(GpioPinListener.class));
        List<GpioPinListener> gpioPinListeners = new ArrayList<>(1);
        gpioPinListeners.add(mock(GpioPinListener.class));
        defaultGpioPinDigital.addListener(gpioPinListeners);
        assertTrue(defaultGpioPinDigital.getListeners().isEmpty());
        assertFalse(defaultGpioPinDigital.hasListener(mock(GpioPinListener.class)));
        defaultGpioPinDigital.removeListener(mock(GpioPinListener.class));
        defaultGpioPinDigital.removeListener(gpioPinListeners);
        defaultGpioPinDigital.removeAllListeners();
        assertNull(defaultGpioPinDigital.getShutdownOptions());
        defaultGpioPinDigital.setShutdownOptions(mock(GpioPinShutdown.class));
        defaultGpioPinDigital.setShutdownOptions(Boolean.TRUE);
        defaultGpioPinDigital.setShutdownOptions(Boolean.TRUE, PinState.HIGH);
        defaultGpioPinDigital.setShutdownOptions(Boolean.TRUE, PinState.HIGH, PinPullResistance.PULL_UP);
        defaultGpioPinDigital.setShutdownOptions(Boolean.TRUE, PinState.HIGH, PinPullResistance.PULL_UP, PinMode.DIGITAL_INPUT);
        assertNull(defaultGpioPinDigital.getShutdownOptions());
        assertTrue(defaultGpioPinDigital.getTriggers().isEmpty());
        defaultGpioPinDigital.addTrigger(mock(GpioTrigger.class));
        List<GpioTrigger> triggers = new ArrayList<>(1);
        triggers.add(mock(GpioTrigger.class));
        defaultGpioPinDigital.addTrigger(triggers);
        assertTrue(defaultGpioPinDigital.getTriggers().isEmpty());
        defaultGpioPinDigital.removeTrigger(triggers);
        defaultGpioPinDigital.removeTrigger(mock(GpioTrigger.class));
        defaultGpioPinDigital.removeAllTriggers();
        assertFalse(defaultGpioPinDigital.isHigh());
        assertFalse(defaultGpioPinDigital.isLow());
        assertNull(defaultGpioPinDigital.getState());
        assertFalse(defaultGpioPinDigital.isState(PinState.HIGH));
        assertFalse(defaultGpioPinDigital.hasDebounce(PinState.HIGH));
        defaultGpioPinDigital.setDebounce(-1);
        defaultGpioPinDigital.setDebounce(-1, PinState.HIGH, PinState.LOW);
        assertEquals(0, defaultGpioPinDigital.getDebounce(PinState.HIGH));
        defaultGpioPinDigital.high();
        defaultGpioPinDigital.low();
        defaultGpioPinDigital.toggle();
        defaultGpioPinDigital.setState(PinState.LOW);
        defaultGpioPinDigital.setState(false);
        assertNull(defaultGpioPinDigital.blink(100));
        assertNull(defaultGpioPinDigital.blink(100, TimeUnit.HOURS));
        assertNull(defaultGpioPinDigital.blink(100, PinState.HIGH));
        assertNull(defaultGpioPinDigital.blink(100, PinState.HIGH, TimeUnit.DAYS));
        assertNull(defaultGpioPinDigital.blink(100, 100));
        assertNull(defaultGpioPinDigital.blink(100, 100, TimeUnit.DAYS));
        assertNull(defaultGpioPinDigital.blink(100, 100, PinState.HIGH));
        assertNull(defaultGpioPinDigital.blink(100, 100, PinState.HIGH, TimeUnit.MICROSECONDS));
        assertNull(defaultGpioPinDigital.pulse(100));
        assertNull(defaultGpioPinDigital.pulse(100, TimeUnit.HOURS));
        assertNull(defaultGpioPinDigital.pulse(100, mock(Callable.class)));
        assertNull(defaultGpioPinDigital.pulse(100, mock(Callable.class), TimeUnit.SECONDS));
        assertNull(defaultGpioPinDigital.pulse(100, false));
        assertNull(defaultGpioPinDigital.pulse(100, false, TimeUnit.MINUTES));
        assertNull(defaultGpioPinDigital.pulse(100, false, mock(Callable.class)));
        assertNull(defaultGpioPinDigital.pulse(100, false, mock(Callable.class), TimeUnit.DAYS));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, TimeUnit.DAYS));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, mock(Callable.class)));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, mock(Callable.class), TimeUnit.DAYS));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, false));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, false, TimeUnit.NANOSECONDS));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, false, mock(Callable.class)));
        assertNull(defaultGpioPinDigital.pulse(100, PinState.LOW, false, mock(Callable.class), TimeUnit.NANOSECONDS));
    }
}
