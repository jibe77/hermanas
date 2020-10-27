package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import java.io.IOException;

public interface GpioHermanasController {

    GpioPinDigitalInput provisionInput(int gpioAddress);

    GpioPinDigitalOutput provisionOutput(int gpioAddress);

    void unprovisionPin(GpioPin pin);

    void moveServo(int doorServoGpioAddress, int positionNumber);

    void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQualityConfig) throws IOException;

    void initSensor(int pinNumber);

    void sendStartSignal(int pinNumber);
}
