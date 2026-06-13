package org.jibe77.hermanas.service.fan;

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

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

@Component
@Scope("singleton")
public class FanService {

    final GpioHermanasService gpioHermanasService;

    @Value("${fan.relay.gpio.address}")
    private int fanRelayGpioAddress;

    @Value("${fan.relay.enabled}")
    private boolean fanEnabled;

    private ConfigService configService;

    DigitalOutput gpioPinDigitalOutput;

    private static final Logger logger = LoggerFactory.getLogger(FanService.class);

    Timer fanSecurityStopTimer;

    ConsumptionModeController consumptionModeController;

    NotificationController notificationController;

    private final EventService eventService;

    public FanService(
            GpioHermanasService gpioHermanasService,
            ConsumptionModeController consumptionModeController,
            NotificationController notificationController,
            ConfigService configService,
            EventService eventService) {
        this.gpioHermanasService = gpioHermanasService;
        this.consumptionModeController = consumptionModeController;
        this.notificationController = notificationController;
        this.configService = configService;
        this.eventService = eventService;
    }

    @PostConstruct
    private void init() {
        if (fanEnabled) {
            gpioPinDigitalOutput = gpioHermanasService.provisionOutput(
                    "fan_relay", "Fan Relay", fanRelayGpioAddress);
        }
    }

    public synchronized void switchOn() {
        switchOn(null);
    }

    /**
     * Switch the fan on with an optional {@code details} string that ends up
     * on the journal entry. Schedulers pass {@code "auto: sunrise"} etc. so
     * operators can tell at a glance which run actually flipped the relay.
     * REST callers go through {@link #switchOn()} which leaves the column
     * empty — the journal then sources attribution from the SecurityContext.
     */
    public synchronized void switchOn(String details) {
        if (fanEnabled) {
            logger.info("Switching on fan.");
            gpioPinDigitalOutput.high();
            startSecurityTimer();
            notificationController.notify(new CoopStatus(Appliance.FAN, StatusEnum.ON));
            eventService.record(EventType.FAN_ON, details);
        }
    }

    private void startSecurityTimer() {
        if (fanSecurityStopTimer != null) {
            fanSecurityStopTimer.cancel();
        }
        long duration = consumptionModeController.getDuration(
                configService.getFanSecurityTimerDelayEco(),
                configService.getFanSecurityTimerDelayRegular(),
                configService.getFanSecurityTimerDelaySunny(),
                LocalDateTime.now());

        fanSecurityStopTimer = new Timer("Fan security stop");
        fanSecurityStopTimer.schedule(new TimerTask() {
                                          public void run() {
                                              logger.info("stopping fan after {} ms.", duration);
                                              switchOff("auto: security timer");
                                          }
                                      },
                duration);
    }

    public synchronized void switchOff() {
        switchOff(null);
    }

    public synchronized void switchOff(String details) {
        if (fanEnabled) {
            logger.info("Switching off fan.");
            gpioPinDigitalOutput.low();
            if (fanSecurityStopTimer != null) {
                fanSecurityStopTimer.cancel();
                fanSecurityStopTimer = null;
            }
            notificationController.notify(new CoopStatus(Appliance.FAN, StatusEnum.OFF));
            eventService.record(EventType.FAN_OFF, details);
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
