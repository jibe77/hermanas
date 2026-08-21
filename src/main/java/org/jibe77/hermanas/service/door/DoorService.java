package org.jibe77.hermanas.service.door;

import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.door.bottombutton.BottomButtonService;
import org.jibe77.hermanas.service.door.model.DoorStatus;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.jibe77.hermanas.service.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.service.door.servo.ServoMotorService;
import org.jibe77.hermanas.service.door.upbutton.UpButtonService;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.websocket.Appliance;
import org.jibe77.hermanas.websocket.CoopStatus;
import org.jibe77.hermanas.websocket.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 * This class controls the servo motor programatically.
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
public class DoorService {

    // the servo motor
    private final ServoMotorService servo;

    private final BottomButtonService bottomButtonService;
    private final UpButtonService upButtonService;

    private final SunTimeManager sunTimeManager;

    private static final Logger logger = LoggerFactory.getLogger(DoorService.class);

    // Servo positions AND durations are sourced from ConfigService so an admin can
    // recalibrate them at runtime from the diagnostic UI. The @Value-injected
    // defaults in ConfigService still come from application.properties.
    private final ConfigService configService;

    private int doorOpeningPosition() { return configService.getDoorOpeningPosition(); }
    private int doorClosingPosition() { return configService.getDoorClosingPosition(); }
    private int doorOpeningDuration() { return configService.getDoorOpeningDuration(); }
    private int doorClosingDuration() { return configService.getDoorClosingDuration(); }

    private LocalDateTime lastClosingTime;
    private LocalDateTime lastOpeningTime;

    private NotificationController notificationController;

    public DoorService(ServoMotorService servo, BottomButtonService bottomButtonService,
                          UpButtonService upButtonService, SunTimeManager sunTimeManager,
                          NotificationController notificationController,
                          ConfigService configService) {
        this.servo = servo;
        this.bottomButtonService = bottomButtonService;
        this.upButtonService = upButtonService;
        this.sunTimeManager = sunTimeManager;
        this.notificationController = notificationController;
        this.configService = configService;
    }

    @PostConstruct
    private synchronized void initDoorAccordingToSunTime() {
        try {
            DoorStatusEnum doorStatusEnum = sunTimeManager.getExpectedDoorStatus();
            if (doorStatusEnum != null) {
                if (doorStatusEnum == DoorStatusEnum.OPENED) {
                    openDoorWithUpButtonManagment(false, false);
                } else if (doorStatusEnum == DoorStatusEnum.CLOSED) {
                    closeDoorWithBottormButtonManagement(false);
                }
            } else {
                logger.warn("Door status is null, cannot initialize door according to sun time.");
            }
        } catch (DoorNotClosedCorrectlyException e) {
            logger.error("The door status couldn't get initialized.", e);
        }
    }

    /**
     * Close the door moving the servomotor clockwise
     * @param force if force is set to true, force door to close even if it is closed.
     */
    @Retryable(
            value = { DoorNotClosedCorrectlyException.class },
            maxAttempts = 1,
            backoff = @Backoff(delay = 2000))
    public synchronized void closeDoorWithBottormButtonManagement(boolean force) {
        if (force || !doorIsClosed()) {
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSING));
            bottomButtonService.provisionButton();
            bottomButtonService.resetBottomButtonState();
            closeDoor(force, true);
            if (bottomPositionReached() || (waitALittle() && bottomPositionReached())) {
                logger.info("bottom position has been reached.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSED));
            } else {
                logger.error("Bottom position not reached correctly. The door is reopened now.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSED_INCORRECTLY));
                // if the door has been closed twice, opening the door is actually closing the door .
                openDoorWithUpButtonManagment(force, true);
                if (!bottomPositionReached())
                    throw new DoorNotClosedCorrectlyException();
            }
            logger.info("... the door has been closed !");
            this.lastClosingTime = LocalDateTime.now();
        } else {
            logger.info("Door is not closed because is already closed state.");
        }
    }

    /**
     * La porte a-t-elle atteint sa position basse ?
     *
     * <p>Deux critères, du plus franc au plus tolérant :</p>
     * <ol>
     *   <li>un appui net a été détecté (niveau haut) — le cas nominal ;</li>
     *   <li>à défaut, <b>n'importe quelle transition</b> sur la ligne du fin de
     *       course bas.</li>
     * </ol>
     *
     * <p>Le second critère existe parce que ce contact est usé : il rebondit
     * abondamment et n'atteint pas toujours un niveau haut franc. Les rebonds
     * étaient alors interprétés comme « position jamais atteinte », déclenchant
     * une réouverture de sécurité alors que la porte était bien en bas.</p>
     *
     * <p>Ce n'est pas laxiste : pendant une fermeture commandée, le seul événement
     * mécanique capable de solliciter ce contact est l'arrivée de la porte. Le
     * drapeau est remis à zéro juste avant le mouvement, donc seule l'activité de
     * <em>cette</em> fermeture est prise en compte — les parasites qui surviennent
     * porte immobile (relevés vers 04 h 45) ne sont jamais lus.</p>
     *
     * <p>⚠️ Contournement d'un défaut matériel, à retirer une fois le contact
     * remplacé. Voir migration.md §5.4.</p>
     */
    private boolean bottomPositionReached() {
        if (bottomButtonService.isBottomButtonHasBeenPressed()) {
            return true;
        }
        if (bottomButtonService.hasBottomButtonChanged()) {
            logger.warn("Aucun appui franc sur le fin de course bas, mais la ligne a bougé "
                    + "pendant la fermeture : position basse considérée comme atteinte. "
                    + "Le contact est probablement use — voir migration.md §5.4.");
            return true;
        }
        return false;
    }

    private boolean waitALittle() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting", e);
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Recover
    private void closeDoorWithoutBottomButtonManagement(DoorNotClosedCorrectlyException e) {
        logger.info("Close door without button management.");
        notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSING));
        closeDoor(false, false);
        notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSED));
    }

    /**
     * Close door.
     * @param force if force is set to true, force door to close even if it is closed.
     */
    protected synchronized void closeDoor(boolean force, boolean addTenPercent) {
        if (force || !doorIsClosed()) {
            logger.info(
                    "Close the door moving servo clockwise with gear position {} for {} ms ...",
                    doorClosingPosition() * (addTenPercent ? 1.1 : 1),
                    doorClosingDuration() * (addTenPercent ? 1.1 : 1));
            servo.setPosition(doorClosingPosition(), doorClosingDuration());
            this.lastClosingTime = LocalDateTime.now();
        } else {
            logger.info("Door is already closing, so the door won't be closed.");
        }
    }

    public synchronized boolean openDoorWithUpButtonManagment(boolean force, boolean openingDoorAfterClosingProblem) {
        boolean returnedValue = false;
        if (force || !doorIsOpened()) {
            upButtonService.provisionButton();
            upButtonService.resetUpButtonState();
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENING));
            // Pass force=true: the guard above already decided the door must move.
            // openDoor() would otherwise re-read the up switch several seconds later
            // (wifi turn-on + camera capture happen in between at sunrise), and a
            // bouncing switch reading "pressed" on that second sample silently
            // cancelled the movement — the door then stayed shut all morning.
            if (openDoor(true, openingDoorAfterClosingProblem) && upButtonService.isUpButtonHasBeenPressed()) {
                logger.info("up position has been reached.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENED));
                returnedValue = true;
            } else {
                logger.info("up button has not been pressed.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENED_INCORRECTLY));
            }
            logger.info("... done");
        } else {
            logger.info("Door is not opened because is already closed state.");
        }
        return returnedValue;
    }

    /**
     * Open the door moving the servomotor counter-clockwise.
     * @param force if force is set to true, force door to open even if it is opened.
     */
    protected synchronized boolean openDoor(boolean force, boolean openingDoorAfterClosingProblem) {
        if (force || openingDoorAfterClosingProblem || !doorIsOpened()) {
            logger.info("Open the door moving servo counterclockwise with gear position {} for {} ms ...",
                    doorOpeningPosition(),
                    doorOpeningDuration());
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENING));
            servo.setPosition(doorOpeningPosition(), doorOpeningDuration());
            if (!openingDoorAfterClosingProblem) {
                this.lastOpeningTime = LocalDateTime.now();
            }
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENED));
            logger.info("... done");
            return true;
        } else {
            logger.info("Door is not opened because is already opened state.");
            return false;
        }
    }

    /**
     * Tells if the door is opened, or probably opened.
     * @return true if the opening time is after the last closing time.
     *          true if the opening or closing time is unknown.
     */
    public synchronized boolean doorIsOpened() {
        return upButtonService.isUpButtonPressed();
    }

    /**
     * Tells if the door is closed, or probably closed.
     * @return true if the closing time is after the last opening time.
     *          true if the opening or closing time is unknown.
     */
    public synchronized boolean doorIsClosed() {
        return bottomButtonService.isBottomButtonPressed();
    }

    public synchronized DoorStatus statusInfo() {
        if (doorIsOpened()) {
            return new DoorStatus(DoorStatusEnum.OPENED, lastOpeningTime);
        } else if (doorIsClosed()) {
            return new DoorStatus(DoorStatusEnum.CLOSED, lastClosingTime);
        } else if (openingTimeIsProbablyTheMostRecent()) {
            logger.info("the door is probably opened but not completly, " +
                    "let's turn the servo counter clockwise a little bit.");
            turnServoCounterClockwise(doorOpeningDuration() / 50);
            if (doorIsOpened() || (waitALittle() && doorIsOpened())) {
                logger.info("the door is completly opened now !");
                return new DoorStatus(DoorStatusEnum.OPENED, lastOpeningTime);
            }
        }
        if (lastOpeningTime == null && lastClosingTime == null) {
            return new DoorStatus(DoorStatusEnum.UNDEFINED, null);
        } else if (lastOpeningTime != null && lastClosingTime == null) {
            return new DoorStatus(DoorStatusEnum.SEEMS_OPENED, lastOpeningTime);
        } else if (lastOpeningTime == null && lastClosingTime != null) {
            return new DoorStatus(DoorStatusEnum.SEEMS_CLOSED, lastClosingTime);
        } else if (lastOpeningTime.isAfter(lastClosingTime)) {
            return new DoorStatus(DoorStatusEnum.SEEMS_OPENED, lastOpeningTime);
        } else {
            return new DoorStatus(DoorStatusEnum.SEEMS_CLOSED, lastClosingTime);
        }
    }

    private synchronized boolean openingTimeIsProbablyTheMostRecent() {
        return lastOpeningTime != null &&
                ((lastClosingTime == null && lastOpeningTime != null) ||
                (lastOpeningTime.isAfter(lastClosingTime)));
    }

    public synchronized void turnServoClockwise(Integer duration) {
        logger.info(
                "Turn the servo clockwise with gear position {} for {} ms ...",
                doorClosingPosition(),
                duration);
        servo.setPosition(doorClosingPosition(), duration);
    }

    public synchronized void turnServoCounterClockwise(Integer duration) {
        logger.info(
                "Turn the servo counter-clockwise with gear position {} for {} ms ...",
                doorOpeningPosition(),
                duration);
        servo.setPosition(doorOpeningPosition(), duration);
    }

    public synchronized void turnServo(int dutyCycle, int frequency, int duration) {
        logger.info("Turn the servo with dutyCycle {} and frequency {} during {} ms ...",
                dutyCycle,
                frequency,
                duration);
        servo.moveServo(dutyCycle, frequency);
        servo.sleepMillisec(duration);
        servo.stop();
    }
}
