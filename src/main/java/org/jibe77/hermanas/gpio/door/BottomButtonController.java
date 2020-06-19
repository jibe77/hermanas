package org.jibe77.hermanas.gpio.door;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import org.jibe77.hermanas.gpio.GpioControllerSingleton;
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
    GpioControllerSingleton gpioControllerSingleton;

    @Value("${door.button.bottom.gpio.address}")
            private int doorButtonBottomGpioAddress;

    private GpioPinDigitalInput bottomButton;

    boolean bottomButtonPressed = false;

    Logger logger = LoggerFactory.getLogger(BottomButtonController.class);

    public BottomButtonController(ServoMotorController servoMotorController, GpioControllerSingleton gpioControllerSingleton) {
        this.servoMotorController = servoMotorController;
        this.gpioControllerSingleton = gpioControllerSingleton;
    }

    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton = gpioControllerSingleton.provisionButton(doorButtonBottomGpioAddress);
            bottomButton.setShutdownOptions(true);
            bottomButton.addListener(new BottomButtonListener(this, servoMotorController));
        }
    }

    public synchronized void unprovisionButton() {
        if (bottomButton != null) {
            logger.info("unprovision door button on gpio instance.");
            bottomButton.removeAllListeners();
            gpioControllerSingleton.unprovisionButton(bottomButton);
            bottomButton = null;
        }
    }

    public boolean isBottomButtonPressed() {
        return bottomButtonPressed;
    }

    protected void setBottomButtonPressed(boolean bottomButtonPressed) {
        this.bottomButtonPressed = bottomButtonPressed;
    }
}
