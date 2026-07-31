package org.jibe77.hermanas.service.gpio;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.pwm.Pwm;
import java.io.File;
import java.io.IOException;

public interface GpioHermanasService {

    DigitalInput provisionInput(String id, String name, int gpioAddress);

    DigitalOutput provisionOutput(String id, String name, int gpioAddress);

    Pwm provisionPwm(String id, String name, int gpioAddress);

    /**
     * Capture une photo dans {@code destination}.
     *
     * <p>Le contrat prend un {@link File} et non un handler picam : la capture passe
     * désormais par {@code rpicam-still}, qui écrit lui-même le fichier. Cela retire
     * aussi picam de l'interface, alors que la librairie native n'existe plus sur
     * arm64/Trixie.</p>
     */
    void takePicture(File destination, boolean highQualityConfig) throws IOException;
}
