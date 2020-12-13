package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.controller.door.upbutton.UpButtonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 * This class controls the servo motor programatically.
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
public class DoorController {

    // the servo motor
    private final ServoMotorController servo;

    final BottomButtonController bottomButtonController;
    final UpButtonController upButtonController;

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

    public DoorController(ServoMotorController servo, BottomButtonController bottomButtonController, UpButtonController upButtonController) {
        this.servo = servo;
        this.bottomButtonController = bottomButtonController;
        this.upButtonController = upButtonController;
    }

    /**
     * Close the door moving the servomotor clockwise
     * @param force if force is set to true, force door to close even if it is closed.
     */
    @Retryable(
            value = { DoorNotClosedCorrectlyException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000))
    public void closeDoorWithBottormButtonManagement(boolean force) {
        if (force || !doorIsClosed()) {
            bottomButtonController.provisionButton();
            bottomButtonController.resetBottomButtonHasBeenPressed();
            closeDoor(force);
            if (bottomButtonController.isBottomButtonHasBeenPressed()) {
                logger.info("bottom position has been reached.");
            } else {
                logger.error("Bottom position not reached correctly. The door is being reopened now.");
                // if the door has been closed twice, opening the door is actually closing the door .
                openDoor(force, true);
                if (!bottomButtonController.isBottomButtonHasBeenPressed())
                    throw new DoorNotClosedCorrectlyException();
            }
            logger.info("... the door has been closed !");
            this.lastClosingTime = LocalDateTime.now();
            bottomButtonController.unprovisionButton();
        } else {
            logger.info("Door is not closed because is already closed state.");
        }
    }

    /**
     * Close door.
     * @param force if force is set to true, force door to close even if it is closed.
     */
    protected void closeDoor(boolean force) {
        if (force || !doorIsClosed()) {
            logger.info(
                    "Close the door moving servo clockwise with gear position {} for {} ms ...",
                    doorClosingPosition,
                    doorClosingDuration);
            servo.setPosition(doorClosingPosition, doorClosingDuration);
            this.lastClosingTime = LocalDateTime.now();
        } else {
            logger.info("Door is already closing, so the door won't be closed.");
        }
    }

    public boolean openDoorWithUpButtonManagment(boolean force, boolean openingDoorAfterClosingProblem) {
        boolean returnedValue = false;
        if (force || !doorIsClosed()) {
            upButtonController.provisionButton();
            upButtonController.resetBottomButtonHasBeenPressed();
            if (openDoor(force, openingDoorAfterClosingProblem) && upButtonController.isUpButtonHasBeenPressed()) {
                logger.info("up position has been reached.");
                returnedValue = true;
            } else {
                logger.info("up button has not been pressed.");
            }
            logger.info("... done");
            upButtonController.unprovisionButton();
        } else {
            logger.info("Door is not opened because is already closed state.");
        }
        return returnedValue;
    }

    /**
     * Open the door moving the servomotor counter-clockwise.
     * @param force if force is set to true, force door to open even if it is opened.
     */
    protected boolean openDoor(boolean force, boolean openingDoorAfterClosingProblem) {
        if (force || !doorIsOpened()) {
            logger.info("Open the door moving servo counterclockwise with gear position {} for {} ms ...",
                    doorOpeningPosition,
                    doorOpeningDuration);
            servo.setPosition(doorOpeningPosition, doorOpeningDuration);
            if (!openingDoorAfterClosingProblem) {
                this.lastOpeningTime = LocalDateTime.now();
            }
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
    public boolean doorIsOpened() {
        logger.info(
                "doorIsOpened() method is comparing last closing time {} with lastOpeningTime {}.",
                lastClosingTime, lastOpeningTime);
        if (lastClosingTime != null && lastOpeningTime != null) {
            return lastOpeningTime.isAfter(lastClosingTime);
        } else if (lastOpeningTime != null) {
            logger.info("The opening time is known but closing time unknown, the door is supposed to be opened.");
            return true;
        } else {
            logger.info("No closing data available, returning false.");
            return false;
        }
    }

    /**
     * Tells if the door is closed, or probably closed.
     * @return true if the closing time is after the last opening time.
     *          true if the opening or closing time is unknown.
     */
    public boolean doorIsClosed() {
        logger.info(
                "doorIsClosed() method is comparing last closing time {} with lastOpeningTime {}.",
                lastClosingTime, lastOpeningTime);
        if (lastClosingTime != null && lastOpeningTime != null) {
            return lastClosingTime.isAfter(lastOpeningTime);
        } else if (lastClosingTime != null) {
            logger.info("The closing time is know but opening time unknown, the door is supposed to be closed.");
            return true;
        } else {
            return false;
        }
    }

    public String status() {
        if (doorIsOpened()) {
            return "OPENED";
        } else if (doorIsClosed()) {
            return "CLOSED";
        } else {
            return "UNDEFINED";
        }
    }

    public void turnServoClockwise(Integer duration) {
        logger.info(
                "Turn the servo clockwise with gear position {} for {} ms ...",
                doorClosingPosition,
                duration);
        servo.setPosition(doorClosingPosition, duration);
    }

    public void turnServoCounterClockwise(Integer duration) {
        logger.info(
                "Turn the servo counter-clockwise with gear position {} for {} ms ...",
                doorOpeningPosition,
                duration);
        servo.setPosition(doorOpeningPosition, duration);
    }
}
