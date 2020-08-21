package org.jibe77.hermanas.gpio.door;

import org.jibe77.hermanas.gpio.door.bottombutton.BottomButtonController;
import org.jibe77.hermanas.gpio.door.servo.ServoMotorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 *A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 *
 * This class controls the servo motor programatically.
 *
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
@Scope("singleton")
public class DoorController
{
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
        logger.info(
                "Close the door moving servo clockwise with gear position {} for {} ms ...",
                doorClosingPosition,
                doorClosingDuration);
        servo.setPosition(doorClosingPosition, doorClosingDuration);
        this.lastClosingTime = LocalDateTime.now();

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
        logger.info("Open the door moving servo counterclockwise with gear position {} for {} ms ...",
                doorOpeningPosition,
                doorOpeningDuration);
        servo.setPosition(doorOpeningPosition, doorOpeningDuration);
        this.lastClosingTime = null;
        logger.info("... done");
    }

    public Optional<LocalDateTime> getLastClosingTime() {
        return Optional.of(lastClosingTime);
    }
}
