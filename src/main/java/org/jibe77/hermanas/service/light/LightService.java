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

    /**
     * Échéance programmée du timer de sécurité, en millisecondes d'epoch, ou
     * {@code null} quand aucun timer n'est armé. Purement diagnostique : elle permet
     * de dire, sur un log, combien de temps il restait à courir au moment où il a
     * été remplacé ou annulé.
     */
    private volatile Long securityTimerFireAt;

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
        } else {
            // Sans cette trace, une demande d'allumage ignorée parce que le relais est
            // désactivé est strictement identique, dans les logs, à une absence de
            // demande. Le texte « Switching on light. » est laissé tel quel plus haut
            // pour ne casser aucun filtre existant.
            logger.warn("Switching on light ignored: light relay is disabled "
                    + "(light.relay.enabled=false), requested by thread {}.",
                    Thread.currentThread().getName());
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
                // Annuler un timer encore valide est l'une des trois façons de laisser
                // la lumière allumée au-delà du délai de sécurité. Sans cette trace on
                // ne peut pas distinguer « le timer a tiré » de « quelqu'un l'a annulé
                // en passant par ici ».
                long remainingMs = securityTimerFireAt == null
                        ? -1 : securityTimerFireAt - System.currentTimeMillis();
                logger.warn("Security timer cancelled by this switchOff (thread {}, details={}); "
                                + "it still had {} ms to run.",
                        Thread.currentThread().getName(), details, remainingMs);
                lightSecurityStopTimer.cancel();
                lightSecurityStopTimer = null;
                securityTimerFireAt = null;
            }
            notificationController.notify(new CoopStatus(Appliance.LIGHT, StatusEnum.OFF));
            eventService.record(EventType.LIGHT_OFF, details);
        } else {
            logger.warn("Switching off light ignored: light relay is disabled "
                    + "(light.relay.enabled=false), requested by thread {}.",
                    Thread.currentThread().getName());
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
            long remainingMs = securityTimerFireAt == null
                    ? -1 : securityTimerFireAt - System.currentTimeMillis();
            logger.warn("Replacing the previous security timer (thread {}); it still had {} ms to run.",
                    Thread.currentThread().getName(), remainingMs);
            lightSecurityStopTimer.cancel();
        }
        lightSecurityStopTimer = new Timer("Light security stop");

        long ecoDelay = configService.getLightSecurityTimerDelayEco();
        long regularDelay = configService.getLightSecurityTimerDelayRegular();
        long sunnyDelay = configService.getLightSecurityTimerDelaySunny();
        long duration = consumptionModeController.getDuration(
                ecoDelay, regularDelay, sunnyDelay, LocalDateTime.now());
        securityTimerFireAt = System.currentTimeMillis() + duration;

        // Le seul log du timer (« stopping light after … ms ») n'est émis que s'il
        // part. On ne pouvait donc pas distinguer, sur un incident : jamais programmé,
        // programmé beaucoup trop loin, ou thread bloqué. Ces trois valeurs + le mode
        // retenu tranchent la question en un coup d'œil.
        logger.info("Security timer armed: mode={}, duration={} ms, expected to fire at {}, "
                        + "raw config eco/regular/sunny = {}/{}/{} ms.",
                consumptionModeController.getCurrentEnergyMode().getCurrentMode(),
                duration,
                LocalDateTime.now().plusNanos(duration * 1_000_000L),
                ecoDelay, regularDelay, sunnyDelay);

        lightSecurityStopTimer.schedule(new TimerTask() {
                                            public void run() {
                                                logger.info("stopping light after {} ms.", duration);
                                                // Une exception non rattrapée dans un TimerTask tue
                                                // son thread en silence : la trace part sur stderr,
                                                // jamais dans spring.log. On la journalise puis on la
                                                // relance telle quelle — le comportement reste
                                                // identique, seule la visibilité change.
                                                try {
                                                    switchOff("auto: security timer");
                                                } catch (RuntimeException e) {
                                                    logger.error("Security timer failed to switch the "
                                                            + "light off; the light may be stuck on.", e);
                                                    throw e;
                                                }
                                            }
                                        },
                duration);
    }
}
