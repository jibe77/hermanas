package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import org.jibe77.hermanas.gpio.door.ServoMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component()
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class GpioSingleton {

    GpioController gpio;

    Logger logger = LoggerFactory.getLogger(GpioSingleton.class);

    @PostConstruct
    private void initialise() {
        logger.info("Initialise GPIO ...");
        gpio = GpioFactory.getInstance();

        /*
         * Before interacting with Pi4J, you must first create a new GPIO
         *  controller instance. The GpioFactory includes a createInstance
         * method to create the GPIO controller. Your project should only
         * instantiate a single GPIO controller instance and that instance
         * should be shared across your project.
         */
        Gpio.wiringPiSetup();
        logger.info("... initialisation done.");
    }

    public GpioController getController() {
        return gpio;
    }

    @PreDestroy
    private void tearDown() {
        logger.info("Shutdown gpio instance.");
        gpio.shutdown();
    }
}
