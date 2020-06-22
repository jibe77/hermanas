package org.jibe77.hermanas.gpio;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;

public interface GpioHermanasController {

    GpioPinDigitalInput provisionInput(int gpioAddress);

    GpioPinDigitalOutput provisionOutput(int gpioAddress);

    void unprovisionPin(GpioPin pin);

    void moveServo(int doorServoGpioAddress, int positionNumber);

}
