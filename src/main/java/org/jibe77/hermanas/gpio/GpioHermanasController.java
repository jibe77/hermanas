package org.jibe77.hermanas.gpio;

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

    void initCamera(int photoWidth, int photoHeight, String photoEncoding, int photoQuality, int photoDelay, int photoRotation);

    void takePicture(FilePictureCaptureHandler filePictureCaptureHandler) throws IOException;

    void initSensor(int pinNumber);

    void sendStartSignal(int pinNumber);

    void waitForResponseSignal(int pinNumber, boolean keepRunning);

    void close(int pinNumber);

    byte[] fetchData(int pinNumber, boolean keepRunning, long startTime);
}
