package org.jibe77.hermanas.service;

import org.jibe77.hermanas.gpio.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.gpio.door.ServoController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Door {

    @Autowired
    ServoController servoController;

    @GetMapping("/door/close")
    public void close() throws DoorNotClosedCorrectlyException {
        servoController.closeDoor();
    }

    @GetMapping("/door/open")
    public void open() {
        servoController.openDoor();
    }
}
