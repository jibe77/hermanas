package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.door.DoorController;
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

    @GetMapping("/door/close")
    public void close() {
        logger.info("closing door now ...");
        doorController.closeDoorWithBottormButtonManagement();
        logger.info("... done !");
    }

    @GetMapping("/door/open")
    public void open() {
        logger.info("opening door now ...");
        doorController.openDoor();
        logger.info("... done !");
    }
}
