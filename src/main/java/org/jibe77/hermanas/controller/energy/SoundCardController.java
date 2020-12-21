package org.jibe77.hermanas.controller.energy;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class SoundCardController {

    ProcessLauncher processLauncher;

    Logger logger = LoggerFactory.getLogger(SoundCardController.class);

    public SoundCardController(ProcessLauncher processLauncher) {
        this.processLauncher = processLauncher;
    }

    @PostConstruct
    private synchronized void init() {
        turnOff();
    }

    public void turnOn() throws IOException {
        processLauncher.launch("/home/pi/usb_on.sh");
    }

    public void turnOff() {
        try {
            processLauncher.launch("/home/pi/usb_off.sh");
        } catch (IOException e) {
            logger.error("Exception when turning off the sound card : ", e);
        }
    }
}
