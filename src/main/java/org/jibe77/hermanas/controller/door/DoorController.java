package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.model.DoorStatus;
import org.jibe77.hermanas.controller.door.model.DoorStatusEnum;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.websocket.Appliance;
import org.jibe77.hermanas.websocket.CoopStatus;
import org.jibe77.hermanas.websocket.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 * This class controls the servo motor programatically.
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
public class DoorController {

    // the servo motor
    private final ServoMotorController servo;

    private final BottomButtonController bottomButtonController;
    private final UpButtonController upButtonController;

    private final SunTimeManager sunTimeManager;

    Logger logger = LoggerFactory.getLogger(DoorController.class);

    @Value("${door.opening.duration}")
    private int doorOpeningDuration;

    @Value("${door.opening.position}")
    private int doorOpeningPosition;

    @Value("${door.closing.duration}")
    private int doorClosingDuration;

    @Value("${door.closing.position}")
    private int doorClosingPosition;

    private LocalDateTime lastClosingTime;
    private LocalDateTime lastOpeningTime;

    private NotificationController notificationController;

    public DoorController(ServoMotorController servo, BottomButtonController bottomButtonController,
                          UpButtonController upButtonController, SunTimeManager sunTimeManager,
                          NotificationController notificationController) {
        this.servo = servo;
        this.bottomButtonController = bottomButtonController;
        this.upButtonController = upButtonController;
        this.sunTimeManager = sunTimeManager;
        this.notificationController = notificationController;
    }

    @PostConstruct
    private synchronized void initDoorAccordingToSunTime() {
        try {
            DoorStatusEnum doorStatusEnum = sunTimeManager.getExpectedDoorStatus();
            if (doorStatusEnum == DoorStatusEnum.OPENED) {
                openDoorWithUpButtonManagment(false, false);
            } else if (doorStatusEnum == DoorStatusEnum.CLOSED) {
                closeDoorWithBottormButtonManagement(false);
            }
        } catch (DoorNotClosedCorrectlyException e) {
            logger.error("The door status couldn't get initialized.");
        }
    }

    /**
     * Close the door moving the servomotor clockwise
     * @param force if force is set to true, force door to close even if it is closed.
     */
    @Retryable(
            value = { DoorNotClosedCorrectlyException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000))
    public synchronized void closeDoorWithBottormButtonManagement(boolean force) {
        if (force || !doorIsClosed()) {
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSING));
            bottomButtonController.provisionButton();
            bottomButtonController.resetBottomButtonState();
            closeDoor(force, true);
            if (bottomButtonController.isBottomButtonHasBeenPressed()
                    || (waitALittle() && bottomButtonController.isBottomButtonHasBeenPressed())) {
                logger.info("bottom position has been reached.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSED));
            } else {
                logger.error("Bottom position not reached correctly. The door is reopened now.");
                notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.CLOSED_INCORRECTLY));
                // if the door has been closed twice, opening the door is actually closing the door .
                openDoorWithUpButtonManagment(force, true);
                if (!bottomButtonController.isBottomButtonHasBeenPressed())
                    throw new DoorNotClosedCorrectlyException();
            }
            logger.info("... the door has been closed !");
            this.lastClosingTime = LocalDateTime.now();
        } else {
            logger.info("Door is not closed because is already closed state.");
        }
    }

    private boolean waitALittle() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                    doorClosingPosition * (addTenPercent ? 1.1 : 1),
                    doorClosingDuration * (addTenPercent ? 1.1 : 1));
            servo.setPosition(doorClosingPosition, doorClosingDuration);
            this.lastClosingTime = LocalDateTime.now();
        } else {
            logger.info("Door is already closing, so the door won't be closed.");
        }
    }

    public synchronized boolean openDoorWithUpButtonManagment(boolean force, boolean openingDoorAfterClosingProblem) {
        boolean returnedValue = false;
        if (force || !doorIsOpened()) {
            upButtonController.provisionButton();
            upButtonController.resetUpButtonState();
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENING));
            if (openDoor(force, openingDoorAfterClosingProblem) && upButtonController.isUpButtonHasBeenPressed()) {
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
                    doorOpeningPosition,
                    doorOpeningDuration);
            notificationController.notify(new CoopStatus(Appliance.DOOR, StatusEnum.OPENING));
            servo.setPosition(doorOpeningPosition, doorOpeningDuration);
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
        return upButtonController.isUpButtonPressed();
    }

    /**
     * Tells if the door is closed, or probably closed.
     * @return true if the closing time is after the last opening time.
     *          true if the opening or closing time is unknown.
     */
    public synchronized boolean doorIsClosed() {
        return bottomButtonController.isBottomButtonPressed();
    }

    public synchronized DoorStatus statusInfo() {
        if (doorIsOpened()) {
            return new DoorStatus(DoorStatusEnum.OPENED, lastOpeningTime);
        } else if (doorIsClosed()) {
            return new DoorStatus(DoorStatusEnum.CLOSED, lastClosingTime);
        } else if (openingTimeIsProbablyTheMostRecent()) {
            logger.info("the door is probably opened but not completly, " +
                    "let's turn the servo counter clockwise a little bit.");
            turnServoCounterClockwise(doorOpeningDuration / 10);
            if (doorIsOpened() || (waitALittle() && doorIsOpened())) {
                logger.info("the door is completly opened now !");
                return new DoorStatus(DoorStatusEnum.OPENED, lastOpeningTime);
            } else {
                logger.info("put it back like it was before.");
                turnServoClockwise(doorClosingDuration / 10);
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
                doorClosingPosition,
                duration);
        servo.setPosition(doorClosingPosition, duration);
    }

    public synchronized void turnServoCounterClockwise(Integer duration) {
        logger.info(
                "Turn the servo counter-clockwise with gear position {} for {} ms ...",
                doorOpeningPosition,
                duration);
        servo.setPosition(doorOpeningPosition, duration);
    }
}
