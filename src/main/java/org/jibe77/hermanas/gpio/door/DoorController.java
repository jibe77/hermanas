package org.jibe77.hermanas.gpio.door;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 *A controller for a servo motor at GPIO pin 1 using software Pulse Width Modulation (Soft PWD).
 *
 * This class controls the servo motor programatically.
 *
 * Class used for BlueJ on Raspberry Pi tutorial.
 */
@Component
public class DoorController
{
    // the servo motor
    private final ServoMotor servo;

    final
    BottomButton bottomButton;

    Logger logger = LoggerFactory.getLogger(DoorController.class);

    @Value("${door.opening.duration}")
    private int doorOpeningDuration;

    @Value("${door.opening.position}")
    private int doorOpeningPosition;

    @Value("${door.closing.duration}")
    private int doorClosingDuration;

    @Value("${door.closing.position}")
    private int doorClosingPosition;

    public DoorController(ServoMotor servo, BottomButton bottomButton) {
        this.servo = servo;
        this.bottomButton = bottomButton;
    }

    /**
     * Close the door moving the servomotor clockwise
     *
     */
    @Retryable(
            value = { DoorNotClosedCorrectlyException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000))
    public void closeDoorWithBottormButtonManagement() throws DoorNotClosedCorrectlyException {
        bottomButton.provisionButton();
        if (!closeDoor()) {
            openDoor();
            throw new DoorNotClosedCorrectlyException();
        }
        logger.info("... done");
        bottomButton.unprovisionButton();
    }

    private boolean closeDoor() {
        logger.info(
                "Close the door moving servo clockwise with gear position {} for {} ms ...",
                doorClosingPosition,
                doorClosingDuration);
        return servo.setPosition(doorClosingPosition, doorClosingDuration);

    }

    @Recover
    private void closeDoorNoError(DoorNotClosedCorrectlyException e) {
        logger.error("The door hasn't been closed correctly, closing it now with bottom button taken in charge.");
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
        logger.info("... done");
    }
}
