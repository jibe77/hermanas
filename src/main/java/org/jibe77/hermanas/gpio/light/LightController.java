package org.jibe77.hermanas.gpio.light;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Scope("singleton")
public class LightController {

    final
    GpioHermanasController gpioHermanasController;

    @Value("${light.relay.gpio.address}")
    private int lightRelayGpioAddress;

    @Value("${light.relay.enabled}")
    private boolean lightEnabled;

    GpioPinDigitalOutput gpioPinDigitalOutput;

    Logger logger = LoggerFactory.getLogger(LightController.class);

    public LightController(GpioHermanasController gpioHermanasController) {
        this.gpioHermanasController = gpioHermanasController;
    }

    @PostConstruct
    private void init() {
        if (lightEnabled)
            gpioPinDigitalOutput = gpioHermanasController.provisionOutput(lightRelayGpioAddress);
    }


    public synchronized void switchOn() {
        if (lightEnabled) {
            logger.info("Switching on light.");
            gpioPinDigitalOutput.high();
        }
    }

    public synchronized void switchOff() {
        if (lightEnabled) {
            logger.info("Switching off light.");
            gpioPinDigitalOutput.low();
        }
    }

    @PreDestroy
    private void tearDown() {
        if (lightEnabled) {
            switchOff();
            gpioHermanasController.unprovisionPin(gpioPinDigitalOutput);
        }
    }
}
