package org.jibe77.hermanas.service.light;

import com.pi4j.io.gpio.digital.DigitalOutput;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.websocket.Appliance;
import org.jibe77.hermanas.websocket.CoopStatus;
import org.jibe77.hermanas.websocket.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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

    private final EventService eventService;

    private static final Logger logger = LoggerFactory.getLogger(LightService.class);

    public LightService(GpioHermanasService gpioHermanasService, ConsumptionModeController consumptionModeController,
                           NotificationController notificationController, ConfigService configService,
                           EventService eventService) {
        this.gpioHermanasService = gpioHermanasService;
        this.consumptionModeController = consumptionModeController;
        this.notificationController = notificationController;
        this.configService = configService;
        this.eventService = eventService;
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
        switchOn(null);
    }

    /**
     * Switch the light on with an optional {@code details} string that ends up
     * on the journal entry. Schedulers pass {@code "auto: before sunset"} so
     * operators can tell at a glance which run actually flipped the relay.
     * REST callers go through {@link #switchOn()} which leaves the column
     * empty — the journal then sources attribution from the SecurityContext.
     */
    public synchronized void switchOn(String details) {
        if (lightEnabled) {
            logger.info("Switching on light.");
            gpioPinDigitalOutput.high();
            startSecurityTimer();
            notificationController.notify(new CoopStatus(Appliance.LIGHT, StatusEnum.ON));
            eventService.record(EventType.LIGHT_ON, details);
        }
    }

    public synchronized void switchOff() {
        switchOff(null);
    }

    /**
     * Switch the light off, recording the optional {@code details} string into
     * the journal entry. Used by {@link #startSecurityTimer()} to mark the row
     * as a timer-driven auto stop so operators can tell at a glance whether the
     * light went off on its own.
     */
    public synchronized void switchOff(String details) {
        if (lightEnabled) {
            logger.info("Switching off light.");
            gpioPinDigitalOutput.low();
            if (lightSecurityStopTimer != null) {
                lightSecurityStopTimer.cancel();
                lightSecurityStopTimer = null;
            }
            notificationController.notify(new CoopStatus(Appliance.LIGHT, StatusEnum.OFF));
            eventService.record(EventType.LIGHT_OFF, details);
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
                                                switchOff("auto: security timer");
                                            }
                                        },
                duration);
    }
}
