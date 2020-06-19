package org.jibe77.hermanas.gpio.door.bottombutton;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.jibe77.hermanas.gpio.door.servo.ServoMotorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class BottomButtonController {

    final
    ServoMotorController servoMotorController;

    final
    GpioHermanasController gpioHermanasController;

    @Value("${door.button.bottom.gpio.address}")
            private int doorButtonBottomGpioAddress;

    private GpioPinDigitalInput bottomButton;

    boolean bottomButtonHasBeenPressed = false;

    Logger logger = LoggerFactory.getLogger(BottomButtonController.class);

    public BottomButtonController(ServoMotorController servoMotorController, GpioHermanasController gpioHermanasController) {
        this.servoMotorController = servoMotorController;
        this.gpioHermanasController = gpioHermanasController;
    }

    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton = gpioHermanasController.provisionButton(doorButtonBottomGpioAddress);
            bottomButton.setShutdownOptions(true);
            bottomButton.addListener(new BottomButtonListener(this, servoMotorController));
        }
    }

    public synchronized void unprovisionButton() {
        resetBottomButtonHasBeenPressed();
        if (bottomButton != null) {
            logger.info("unprovision door button on gpio instance.");
            bottomButton.removeAllListeners();
            gpioHermanasController.unprovisionButton(bottomButton);
            bottomButton = null;
        }
    }

    public void resetBottomButtonHasBeenPressed() {
        bottomButtonHasBeenPressed = false;
    }

    public boolean isBottomButtonHasBeenPressed() {
        return bottomButtonHasBeenPressed;
    }

    protected void setBottomButtonHasBeenPressed(boolean bottomButtonHasBeenPressed) {
        this.bottomButtonHasBeenPressed = bottomButtonHasBeenPressed;
    }
}
