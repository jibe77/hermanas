package org.jibe77.hermanas.service.door.bottombutton;

import com.pi4j.io.gpio.digital.DigitalInput;
import org.jibe77.hermanas.service.door.servo.ServoMotorService;
import org.jibe77.hermanas.service.gpio.GpioHermanasService;
import org.jibe77.hermanas.websocket.Button;
import org.jibe77.hermanas.websocket.ButtonNotificationController;
import org.jibe77.hermanas.websocket.ButtonStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BottomButtonService {

    final GpioHermanasService gpioHermanasService;

    final ServoMotorService servoMotorService;

    final ButtonNotificationController buttonNotificationController;

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomGpioAddress;

    private DigitalInput bottomButton;


    boolean bottomButtonHasBeenPressed = false;

    private static final Logger logger = LoggerFactory.getLogger(BottomButtonService.class);

    public BottomButtonService(GpioHermanasService gpioHermanasService,
                               ServoMotorService servoMotorService,
                               ButtonNotificationController buttonNotificationController) {
        this.gpioHermanasService = gpioHermanasService;
        this.servoMotorService = servoMotorService;
        this.buttonNotificationController = buttonNotificationController;
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
                boolean pressed = event.state().isHigh();
                if (pressed) {
                    logger.info("Door has reached the bottom, stop servomotor now !");
                    this.bottomButtonHasBeenPressed = true;
                    servoMotorService.stop();
                } else if (event.state().isLow()) {
                    logger.info("Bottom button is not pressed anymore.");
                }
                buttonNotificationController.notify(
                        new ButtonStatus(Button.BOTTOM, pressed, System.currentTimeMillis()));
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
