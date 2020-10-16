package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
    public boolean close() {
        logger.info("closing door now ...");
        try {
            doorController.closeDoorWithBottormButtonManagement();
            logger.info("... done !");
            return true;
        } catch (DoorNotClosedCorrectlyException e) {
            logger.error("Door could not be closed correctly.");
            return false;
        }
    }

    @GetMapping("/door/open")
    public void open() {
        logger.info("opening door now ...");
        doorController.openDoor();
        logger.info("... done !");
    }

    @GetMapping("/door/status")
    public String status() {
        if (doorController.doorIsOpened() && !doorController.doorIsClosed()) {
            return "OPENED";
        } else if (doorController.doorIsClosed() && !doorController.doorIsOpened()) {
            return "CLOSED";
        } else {
            return "UNDEFINED";
        }
    }
}
