package org.jibe77.hermanas.controller.gpio;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.pwm.Pwm;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import java.io.IOException;

public interface GpioHermanasController {

    DigitalInput provisionInput(String id, String name, int gpioAddress);

    DigitalOutput provisionOutput(String id, String name, int gpioAddress);

    Pwm provisionPwm(String id, String name, int gpioAddress);

    void takePicture(FilePictureCaptureHandler filePictureCaptureHandler, boolean highQualityConfig) throws IOException;
}
