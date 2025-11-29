package org.jibe77.hermanas.controller.door.bottombutton;

import com.pi4j.io.gpio.digital.DigitalInput;
import org.jibe77.hermanas.controller.door.servo.ServoMotorService;
import org.jibe77.hermanas.controller.gpio.GpioHermanasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BottomButtonService {

    final GpioHermanasService gpioHermanasService;

    final ServoMotorService servoMotorService;

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomGpioAddress;

    private DigitalInput bottomButton;


    boolean bottomButtonHasBeenPressed = false;

    private static final Logger logger = LoggerFactory.getLogger(BottomButtonService.class);

    public BottomButtonService(GpioHermanasService gpioHermanasService, ServoMotorService servoMotorService) {
        this.gpioHermanasService = gpioHermanasService;
        this.servoMotorService = servoMotorService;
    }

    @PostConstruct
    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton = gpioHermanasService.provisionInput(
                    "door_bottom_button",
                    "Door bottom button",
                    doorButtonBottomGpioAddress);
            bottomButton.addListener(event -> {
                if (event.state().isHigh()) {
                    logger.info("Door has reached the bottom, stop servomotor now !");
                    this.bottomButtonHasBeenPressed = true;
                    servoMotorService.stop();
                } else if (event.state().isLow()) {
                    logger.info("Bottom button is not pressed anymore.");
                }
            });
        }
    }

    public void resetBottomButtonState() {
        bottomButtonHasBeenPressed = false;
    }

    public boolean isBottomButtonHasBeenPressed() {
        return bottomButtonHasBeenPressed;
    }

    public synchronized boolean isBottomButtonPressed() {
        return bottomButton.isHigh();
    }
}
