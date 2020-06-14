package org.jibe77.hermanas.gpio.door;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ServoController
{
    // the servo motor
    private final ServoMotor servo;

    final
    BottomButton bottomButton;

    Logger logger = LoggerFactory.getLogger(ServoController.class);

    private static final int DOOR_OPENING_DURATION = 8000;
    private static final int DOOR_OPENING_GEAR_POSITION = 16;

    private static final int DOOR_CLOSING_DURATION = 3000;
    private static final int DOOR_CLOSING_GEAR_POSITION = 5;

    public ServoController(ServoMotor servo, BottomButton bottomButton) {
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
    public void closeDoor() throws DoorNotClosedCorrectlyException {
        //
        bottomButton.provisionButton();
        logger.info(
                "Close the door moving servo clockwise with gear position {} for {} ms ...",
                DOOR_CLOSING_GEAR_POSITION,
                DOOR_CLOSING_DURATION);
        boolean bottomButtonPressed = servo.setPosition(DOOR_CLOSING_GEAR_POSITION, DOOR_CLOSING_DURATION);
        if (!bottomButtonPressed) {
            openDoor();
            throw new DoorNotClosedCorrectlyException();
        }
        logger.info("... done");
        bottomButton.unprovisionButton();
    }

    @Recover
    private void closeDoorNoError(DoorNotClosedCorrectlyException e) {
        logger.error("The door hasn't been closed correctly, closing it now with bottom button taken in charge.");
        servo.setPosition(DOOR_CLOSING_GEAR_POSITION, DOOR_CLOSING_DURATION);
    }

    /**
     * Open the door moving the servomotor counter-clockwise.
     *
     */
    public void openDoor()
    {
        logger.info("Open the door moving servo counterclockwise with gear position {} for {} ms ...",
                DOOR_OPENING_GEAR_POSITION,
                DOOR_OPENING_DURATION);
        servo.setPosition(DOOR_OPENING_GEAR_POSITION, DOOR_OPENING_DURATION);
        logger.info("... done");
    }
}
