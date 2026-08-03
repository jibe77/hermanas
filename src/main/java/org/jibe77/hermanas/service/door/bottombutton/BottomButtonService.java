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

import jakarta.annotation.PostConstruct;

@Component
public class BottomButtonService {

    final GpioHermanasService gpioHermanasService;

    final ServoMotorService servoMotorService;

    final ButtonNotificationController buttonNotificationController;

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomGpioAddress;

    private DigitalInput bottomButton;


    boolean bottomButtonHasBeenPressed = false;

    /**
     * Toute transition observée sur la ligne depuis le dernier
     * {@link #resetBottomButtonState()}, pressions comme relâchements.
     *
     * <p>Le contact de fin de course bas est usé : il rebondit, parfois des dizaines
     * de fois d'affilée, et n'atteint pas toujours un niveau haut franc. S'en tenir
     * à {@link #bottomButtonHasBeenPressed} laissait alors croire que la porte
     * n'était jamais arrivée en bas, d'où des réouvertures de sécurité à répétition.</p>
     *
     * <p>Pendant une fermeture commandée, une agitation quelconque de ce contact
     * signifie que la porte l'a atteint : c'est le seul événement mécanique capable
     * de le solliciter. {@link org.jibe77.hermanas.service.door.DoorService} s'en
     * sert donc comme second critère.</p>
     *
     * <p>Volatile : écrit depuis le thread d'événements pi4j, lu depuis le thread
     * qui pilote la porte.</p>
     */
    private volatile boolean bottomButtonHasChanged = false;

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
                // Toute transition compte, y compris un relâchement : sur un contact
                // qui rebondit, le niveau haut n'est pas toujours vu.
                this.bottomButtonHasChanged = true;
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
        bottomButtonHasChanged = false;
    }

    public boolean isBottomButtonHasBeenPressed() {
        return bottomButtonHasBeenPressed;
    }

    /**
     * Vrai si la ligne a bougé depuis le dernier {@link #resetBottomButtonState()},
     * dans un sens ou dans l'autre.
     *
     * <p>À n'interroger que <b>pendant une fermeture commandée</b> : ce contact
     * s'agite aussi tout seul, porte immobile (relevé du 2026-08-03 à 04 h 45).
     * Hors de cette fenêtre, une transition ne prouve rien.</p>
     */
    public boolean hasBottomButtonChanged() {
        return bottomButtonHasChanged;
    }

    public synchronized boolean isBottomButtonPressed() {
        return bottomButton.isHigh();
    }
}
