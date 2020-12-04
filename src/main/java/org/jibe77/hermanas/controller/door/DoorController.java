package org.jibe77.hermanas.controller.door;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.door.servo.ServoMotorController;
import org.jibe77.hermanas.image.DoorPictureAnalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

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

    private final CameraController cameraController;

    final DoorPictureAnalizer doorPictureAnalizer;

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

    public DoorController(ServoMotorController servo, DoorPictureAnalizer doorPictureAnalizer, CameraController cameraController) {
        this.servo = servo;
        this.doorPictureAnalizer = doorPictureAnalizer;
        this.cameraController = cameraController;
    }

    /**
     * Close door.
     * @param force if force is set to true, force door to close even if it is closed.
     */
    public void closeDoor(boolean force) {
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

    /**
     * Open the door moving the servomotor counter-clockwise.
     * @param force if force is set to true, force door to open even if it is opened.
     */
    public boolean openDoor(boolean force, boolean openingDoorAfterClosingProblem) {
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

    public int getClosingRate() {
        logger.info("Returning the door closing rate.");
        Optional<File> picture = cameraController.takePictureNoException(true);
        if (picture.isPresent()) {
            try {
                int result = doorPictureAnalizer.getClosedStatus(picture.get());
                logger.info("return {}.", result);
                return result;
            } catch (IOException e) {
                logger.error("Can't read picture.", e);
            }
        }
        return doorIsClosed() ? 100 : 0;
    }
}
