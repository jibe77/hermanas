package org.jibe77.hermanas.controller.fan;

import com.pi4j.io.gpio.digital.DigitalOutput;
import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.config.ConfigController;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.websocket.Appliance;
import org.jibe77.hermanas.websocket.CoopStatus;
import org.jibe77.hermanas.websocket.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
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

    private ConfigController configController;

    DigitalOutput gpioPinDigitalOutput;

    Logger logger = LoggerFactory.getLogger(FanController.class);

    Timer fanSecurityStopTimer;

    ConsumptionModeController consumptionModeController;

    NotificationController notificationController;

    public FanController(
            GpioHermanasController gpioHermanasController,
            ConsumptionModeController consumptionModeController,
            NotificationController notificationController,
            ConfigController configController) {
        this.gpioHermanasController = gpioHermanasController;
        this.consumptionModeController = consumptionModeController;
        this.notificationController = notificationController;
        this.configController = configController;
    }

    @PostConstruct
    private void init() {
        if (fanEnabled) {
            gpioPinDigitalOutput = gpioHermanasController.provisionOutput(
                    "fan_relay", "Fan Relay", fanRelayGpioAddress);
        }
    }

    public synchronized void switchOn() {
        if (fanEnabled) {
            logger.info("Switching on fan.");
            gpioPinDigitalOutput.high();
            startSecurityTimer();
            notificationController.notify(new CoopStatus(Appliance.FAN, StatusEnum.ON));
        }
    }

    private void startSecurityTimer() {
        if (fanSecurityStopTimer != null) {
            fanSecurityStopTimer.cancel();
        }
        long duration = consumptionModeController.getDuration(
                configController.getFanSecurityTimerDelayEco(),
                configController.getFanSecurityTimerDelayRegular(),
                configController.getFanSecurityTimerDelaySunny(),
                LocalDateTime.now());
        
        fanSecurityStopTimer = new Timer("Fan security stop");
        fanSecurityStopTimer.schedule(new TimerTask() {
                                          public void run() {
                                              logger.info("stopping fan after {} ms.", duration);
                                              switchOff();
                                          }
                                      },
                duration);
    }

    public synchronized void switchOff() {
        if (fanEnabled) {
            logger.info("Switching off fan.");
            gpioPinDigitalOutput.low();
            if (fanSecurityStopTimer != null) {
                fanSecurityStopTimer.cancel();
                fanSecurityStopTimer = null;
            }
            notificationController.notify(new CoopStatus(Appliance.FAN, StatusEnum.OFF));
        }
    }

    /**
     * if the light is enabled and the pin is high, then returns true
     *
     * @return true if the light is on.
     */
    public Status getStatus() {
        return new Status(
         fanEnabled &&
                gpioPinDigitalOutput.state() != null &&
                gpioPinDigitalOutput.state().isHigh() ? StatusEnum.ON : StatusEnum.OFF, -1);
    }

    public Status switcher(boolean param) {
        if (param) {
            switchOn();
        } else {
            switchOff();
        }
        return getStatus();
    }
}
