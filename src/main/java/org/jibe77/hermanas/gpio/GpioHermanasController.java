package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.GpioPinDigitalInput;

public interface GpioHermanasController {

    GpioPinDigitalInput provisionButton(int gpioAddress);

    void unprovisionButton(GpioPinDigitalInput bottomButton);

    void moveServo(int doorServoGpioAddress, int positionNumber);
}
