package org.jibe77.hermanas.service;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.DoorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DoorService {

    @Autowired
    DoorController servoController;

    @GetMapping("/door/close")
    public void close() throws DoorNotClosedCorrectlyException {
        servoController.closeDoorWithBottormButtonManagement();
    }

    @GetMapping("/door/open")
    public void open() {
        servoController.openDoor();
    }
}
