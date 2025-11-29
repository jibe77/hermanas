package org.jibe77.hermanas.controller;

import org.jibe77.hermanas.service.door.DoorController;
import org.jibe77.hermanas.service.door.model.DoorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController
@Service
@Validated
public class DoorService {

    DoorController doorController;

    private static final Logger logger = LoggerFactory.getLogger(DoorService.class);

    public DoorService(DoorController doorController) {
        this.doorController = doorController;
    }

    /**
     * Close the door.
     * @return true if the bottom button has been pressed,
     *          false if the door has been closed without touching the bottom button.
     */
    @PostMapping("/door/close")
    public boolean close(@RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("closing door now  ...");
        doorController.closeDoorWithBottormButtonManagement(Boolean.parseBoolean(force));
        logger.info("... the door has been closed !");
        return true;
    }

    @PostMapping("/door/open")
    public boolean open(@RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("opening door now  ...");
        boolean result = doorController.openDoorWithUpButtonManagment(Boolean.parseBoolean(force), false);
        logger.info("... done with result {} !", result);
        return result;
    }

    @GetMapping("/door/turnClockwise")
    public String turnClockwise(
            @RequestParam(defaultValue = "50", required = false)
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor clockwise  ...");
        doorController.turnServoClockwise(duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/turnCounterClockwise")
    public String turnCounterClockwise(
            @RequestParam(defaultValue = "50", required = false)
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorController.turnServoCounterClockwise(duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/turnServo")
    public String turnServo(
            @RequestParam
            @Min(value = 0, message = "Duty cycle must be between 0 and 100")
            @Max(value = 100, message = "Duty cycle must be between 0 and 100")
            int dutyCycle,
            @RequestParam
            @Min(value = 1, message = "Frequency must be at least 1Hz")
            @Max(value = 1000, message = "Frequency cannot exceed 1000Hz")
            int frequency,
            @RequestParam
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorController.turnServo(dutyCycle, frequency, duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/status")
    public DoorStatus statusInfo() {
        return doorController.statusInfo();
    }
}
