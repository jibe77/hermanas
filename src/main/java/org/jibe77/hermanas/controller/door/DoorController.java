package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 * This class controls the servo motor programatically.
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
@Scope("singleton")
public class DoorController {

    // the servo motor
    private final ServoMotorController servo;

    final BottomButtonController bottomButtonController;

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

    public DoorController(ServoMotorController servo, BottomButtonController bottomButtonController) {
        this.servo = servo;
        this.bottomButtonController = bottomButtonController;
    }

    /**
     * Close the door moving the servomotor clockwise
     *
     */
    @Retryable(
            value = { DoorNotClosedCorrectlyException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000))
    public void closeDoorWithBottormButtonManagement() {
        bottomButtonController.provisionButton();
        bottomButtonController.resetBottomButtonHasBeenPressed();
        closeDoor();
        if (!bottomButtonController.isBottomButtonHasBeenPressed()) {
            logger.error("Bottom position not reached correctly. The door is being reopened now.");
            openDoor();
            // if the door has been closed twice, opening the door is actually closing the door .
            if(!bottomButtonController.isBottomButtonHasBeenPressed())
                throw new DoorNotClosedCorrectlyException();
        }
        this.lastClosingTime = LocalDateTime.now();
        logger.info("... done");
        bottomButtonController.unprovisionButton();
    }

    private void closeDoor() {
        if (doorIsOpened()) {
            logger.info(
                    "Close the door moving servo clockwise with gear position {} for {} ms ...",
                    doorClosingPosition,
                    doorClosingDuration);
            servo.setPosition(doorClosingPosition, doorClosingDuration);
            this.lastClosingTime = LocalDateTime.now();
        }
    }

    @Recover
    private void closeDoorNoError(DoorNotClosedCorrectlyException e) {
        logger.error("The door hasn't been closed correctly, closing it now with bottom button taken in charge.", e);
        closeDoor();
    }

    /**
     * Open the door moving the servomotor counter-clockwise.
     *
     */
    public void openDoor()
    {
        if (doorIsClosed()) {
            logger.info("Open the door moving servo counterclockwise with gear position {} for {} ms ...",
                    doorOpeningPosition,
                    doorOpeningDuration);
            servo.setPosition(doorOpeningPosition, doorOpeningDuration);
            this.lastOpeningTime = LocalDateTime.now();
            logger.info("... done");
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
        } else if (lastOpeningTime != null && lastClosingTime == null) {
            logger.info("The opening time is know but closing time unknown, the door is supposed to be opened.");
          return true;
        } else {
            logger.info("Some data is missing so the door is supposed to be opened.");
            return true;
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
        } else if (lastClosingTime != null && lastOpeningTime == null) {
            logger.info("The closing time is know but opening time unknown, the door is supposed to be closed.");
            return true;
        } else {
            logger.info("Some data is missing so the door is supposed to be closed.");
            return true;
        }
    }
}
