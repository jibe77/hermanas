package org.jibe77.hermanas.controller.light;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Timer;
import java.util.TimerTask;

@Component
@Scope("singleton")
public class LightController {

    final
    GpioHermanasController gpioHermanasController;

    @Value("${light.relay.gpio.address}")
    private int lightRelayGpioAddress;

    @Value("${light.relay.enabled}")
    private boolean lightEnabled;

    @Value("${light.security.timer.delay.eco}")
    private long lightSecurityTimerDelayEco;

    @Value("${light.security.timer.delay.regular}")
    private long lightSecurityTimerDelayRegular;

    @Value("${light.security.timer.delay.sunny}")
    private long lightSecurityTimerDelaySunny;

    private Timer lightSecurityStopTimer;

    GpioPinDigitalOutput gpioPinDigitalOutput;

    ConsumptionModeManager consumptionModeManager;

    Logger logger = LoggerFactory.getLogger(LightController.class);

    public LightController(GpioHermanasController gpioHermanasController, ConsumptionModeManager consumptionModeManager) {
        this.gpioHermanasController = gpioHermanasController;
        this.consumptionModeManager = consumptionModeManager;
    }

    @PostConstruct
    private void init() {
        if (lightEnabled) {
            logger.info("initialising light relay on gpio pin {}.", lightRelayGpioAddress);
            gpioPinDigitalOutput = gpioHermanasController.provisionOutput(lightRelayGpioAddress);
        }
    }

    public synchronized Status switcher(boolean param) {
        if (param) {
            switchOn();
        } else {
            switchOff();
        }
        return getStatus();
    }

    public synchronized void switchOn() {
        if (lightEnabled) {
            logger.info("Switching on light.");
            gpioPinDigitalOutput.high();
            startSecurityTimer();
        }
    }

    public synchronized void switchOff() {
        if (lightEnabled) {
            logger.info("Switching off light.");
            gpioPinDigitalOutput.low();
            if (lightSecurityStopTimer != null) {
                lightSecurityStopTimer.cancel();
                lightSecurityStopTimer = null;
            }
        }
    }

    /**
     * if the light is enabled and the pin is high, then returns true
     * @return true if the light is on.
     */
    public Status getStatus() {
        return new Status(lightEnabled &&
                gpioPinDigitalOutput.getState() != null &&
                gpioPinDigitalOutput.getState().isHigh() ? StatusEnum.ON : StatusEnum.OFF, -1);
    }

    private void startSecurityTimer() {
        if (lightSecurityStopTimer != null) {
            lightSecurityStopTimer.cancel();
        }
        lightSecurityStopTimer = new Timer("Light security stop");
        long duration = consumptionModeManager.getDuration(
                lightSecurityTimerDelayEco, lightSecurityTimerDelayRegular, lightSecurityTimerDelaySunny);
        lightSecurityStopTimer.schedule(new TimerTask() {
                                            public void run() {
                                                logger.info("stopping light after {} ms.", duration);
                                                switchOff();
                                            }
                                        },
                duration);
    }

    @PreDestroy
    private void tearDown() {
        if (lightEnabled) {
            switchOff();
            gpioHermanasController.unprovisionPin(gpioPinDigitalOutput);
        }
    }
}
