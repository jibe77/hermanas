package org.jibe77.hermanas.service;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.DoorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DoorService {

    @Autowired
    DoorController servoController;

    Logger logger = LoggerFactory.getLogger(DoorService.class);

    @GetMapping("/door/close")
    public void close() throws DoorNotClosedCorrectlyException {
        logger.info("closing door now ...");
        servoController.closeDoorWithBottormButtonManagement();
        logger.info("... done !");
    }

    @GetMapping("/door/open")
    public void open() {
        logger.info("opening door now ...");
        servoController.openDoor();
        logger.info("... done !");
    }
}
