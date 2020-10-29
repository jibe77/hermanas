package org.jibe77.hermanas.controller.fan;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
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
public class FanController {

    final GpioHermanasController gpioHermanasController;

    @Value("${fan.relay.gpio.address}")
    private int fanRelayGpioAddress;

    @Value("${fan.relay.enabled}")
    private boolean fanEnabled;

    @Value("${fan.security.timer.delay}")
    private long fanSecurityTimerDelay;

    GpioPinDigitalOutput gpioPinDigitalOutput;

    Logger logger = LoggerFactory.getLogger(FanController.class);

    Timer fanSecurityStopTimer;

    public FanController(GpioHermanasController gpioHermanasController) {
        this.gpioHermanasController = gpioHermanasController;
    }

    @PostConstruct
    private void init() {
        if (fanEnabled) {
            gpioPinDigitalOutput = gpioHermanasController.provisionOutput(fanRelayGpioAddress);
        }
    }

    public synchronized void switchOn() {
        if (fanEnabled) {
            logger.info("Switching on ir light.");
            gpioPinDigitalOutput.high();
            startSecurityTimer();
        }

    }

    private void startSecurityTimer() {
        if (fanSecurityStopTimer != null) {
            fanSecurityStopTimer.cancel();
        }
        fanSecurityStopTimer = new Timer("Fan security stop");
        fanSecurityStopTimer.schedule(new TimerTask() {
                                          public void run() {
                                              logger.info("stopping fan after {} ms.", fanSecurityTimerDelay);
                                              switchOff();
                                          }
                                      },
                fanSecurityTimerDelay);
    }

    public synchronized void switchOff() {
        if (fanEnabled) {
            logger.info("Switching off ir light.");
            gpioPinDigitalOutput.low();
            if (fanSecurityStopTimer != null) {
                fanSecurityStopTimer.cancel();
                fanSecurityStopTimer = null;
            }
        }
    }

    @PreDestroy
    private void tearDown() {
        if (fanEnabled) {
            switchOff();
            gpioHermanasController.unprovisionPin(gpioPinDigitalOutput);
        }
    }

    /**
     * if the light is enabled and the pin is high, then returns true
     *
     * @return true if the light is on.
     */
    public boolean isSwitchedOn() {
        return fanEnabled &&
                gpioPinDigitalOutput.getState() != null &&
                gpioPinDigitalOutput.getState().isHigh();
    }
}
