package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.model.DoorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Service
public class DoorService {

    DoorController doorController;

    Logger logger = LoggerFactory.getLogger(DoorService.class);

    public DoorService(DoorController doorController) {
        this.doorController = doorController;
    }

    /**
     * Close the door.
     * @return true if the bottom button has been pressed,
     *          false if the door has been closed without touching the bottom button.
     */
    @GetMapping("/door/close")
    public boolean close(@RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("closing door now  ...");
        doorController.closeDoorWithBottormButtonManagement(Boolean.parseBoolean(force));
        logger.info("... the door has been closed !");
        return true;
    }

    @GetMapping("/door/open")
    public boolean open(@RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("opening door now  ...");
        boolean result = doorController.openDoorWithUpButtonManagment(Boolean.parseBoolean(force), false);
        logger.info("... done with result {} !", result);
        return result;
    }

    @GetMapping("/door/turnClockwise")
    public String turnClockwise(@RequestParam(defaultValue = "50", required = false) String duration) {
        logger.info("turning servomotor clockwise  ...");
        doorController.turnServoClockwise(Integer.valueOf(duration));
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/turnCounterClockwise")
    public String turnCounterClockwise(@RequestParam(defaultValue = "50", required = false) String duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorController.turnServoCounterClockwise(Integer.valueOf(duration));
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/turnCounterClockwise")
    public String turnCounterClockwise(String dutyCycle, String frequency, String duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorController.turnServo(
                Integer.valueOf(dutyCycle),
                Integer.valueOf(frequency),
                Integer.valueOf(duration));
        logger.info("... servomotor done !");
        return "done";
    }

    @GetMapping("/door/status")
    public DoorStatus statusInfo() {
        return doorController.statusInfo();
    }
}
