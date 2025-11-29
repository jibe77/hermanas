package org.jibe77.hermanas.controller.light;

import com.pi4j.io.gpio.digital.DigitalOutput;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.config.ConfigService;
import org.jibe77.hermanas.controller.gpio.GpioHermanasService;
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
public class LightService {

    final
    GpioHermanasService gpioHermanasService;

    @Value("${light.relay.gpio.address}")
    private int lightRelayGpioAddress;

    @Value("${light.relay.enabled}")
    private boolean lightEnabled;

    NotificationController notificationController;

    private Timer lightSecurityStopTimer;

    private ConfigService configService;

    DigitalOutput gpioPinDigitalOutput;

    ConsumptionModeController consumptionModeController;

    private static final Logger logger = LoggerFactory.getLogger(LightService.class);

    public LightService(GpioHermanasService gpioHermanasService, ConsumptionModeController consumptionModeController,
                           NotificationController notificationController, ConfigService configService) {
        this.gpioHermanasService = gpioHermanasService;
        this.consumptionModeController = consumptionModeController;
        this.notificationController = notificationController;
        this.configService = configService;
    }

    @PostConstruct
    private void init() {
        if (lightEnabled && gpioPinDigitalOutput == null) {
            logger.info("initialising light relay on gpio pin {}.", lightRelayGpioAddress);
            gpioPinDigitalOutput = gpioHermanasService.provisionOutput(
                    "light_relay", "Light relay", lightRelayGpioAddress);
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
            notificationController.notify(new CoopStatus(Appliance.LIGHT, StatusEnum.ON));
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
            notificationController.notify(new CoopStatus(Appliance.LIGHT, StatusEnum.OFF));
        }
    }

    /**
     * if the light is enabled and the pin is high, then returns true
     * @return true if the light is on.
     */
    public Status getStatus() {
        return new Status(lightEnabled &&
                gpioPinDigitalOutput.state() != null &&
                gpioPinDigitalOutput.state().isHigh() ? StatusEnum.ON : StatusEnum.OFF, -1);
    }

    private void startSecurityTimer() {
        if (lightSecurityStopTimer != null) {
            lightSecurityStopTimer.cancel();
        }
        lightSecurityStopTimer = new Timer("Light security stop");
        long duration = consumptionModeController.getDuration(
                configService.getLightSecurityTimerDelayEco(),
                configService.getLightSecurityTimerDelayRegular(),
                configService.getLightSecurityTimerDelaySunny(),
                LocalDateTime.now());
        lightSecurityStopTimer.schedule(new TimerTask() {
                                            public void run() {
                                                logger.info("stopping light after {} ms.", duration);
                                                switchOff();
                                            }
                                        },
                duration);
    }
}
