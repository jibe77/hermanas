package org.jibe77.hermanas.gpio.door;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import org.jibe77.hermanas.gpio.GpioControllerSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class BottomButton {

    final
    ServoMotor servoMotor;

    final
    GpioControllerSingleton gpioControllerSingleton;

    GpioPinDigitalInput bottomButton;

    Logger logger = LoggerFactory.getLogger(BottomButton.class);

    public BottomButton(ServoMotor servoMotor, GpioControllerSingleton gpioControllerSingleton) {
        this.servoMotor = servoMotor;
        this.gpioControllerSingleton = gpioControllerSingleton;
    }

    public synchronized void provisionButton() {
        if (bottomButton == null) {
            logger.info("provision door button on gpio instance.");
            bottomButton =
                    gpioControllerSingleton.getController().provisionDigitalInputPin(
                            RaspiPin.GPIO_16, PinPullResistance.PULL_DOWN);
            bottomButton.setShutdownOptions(true);
            bottomButton.addListener(new BottomButtonListener(servoMotor));
        }
    }

    public synchronized void unprovisionButton() {
        if (bottomButton != null) {
            logger.info("unprovision door button on gpio instance.");
            gpioControllerSingleton.getController().unprovisionPin(bottomButton);
            bottomButton.removeAllListeners();
            bottomButton = null;
        }
    }


}
