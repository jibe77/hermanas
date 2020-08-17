package org.jibe77.hermanas.gpio.camera;

import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.data.entity.Picture;
import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.jibe77.hermanas.gpio.light.LightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Scope("singleton")
public class CameraController {

    private LightController lightController;

    private GpioHermanasController gpioHermanasController;

    private PictureRepository pictureRepository;

    @Value("${camera.width}")
    private int photoWidth;

    @Value("${camera.height}")
    private int photoHeight;

    @Value("${camera.encoding}")
    private String photoEncoding;

    @Value("${camera.quality}")
    private int photoQuality;

    @Value("${camera.delay}")
    private int photoDelay;

    @Value("${camera.rotation}")
    private int photoRotation;

    @Value("${camera.path.root}")
    private String rootPath;

    Logger logger = LoggerFactory.getLogger(CameraController.class);

    public CameraController(LightController lightController, GpioHermanasController gpioHermanasController, PictureRepository pictureRepository) {
        this.lightController = lightController;
        this.gpioHermanasController = gpioHermanasController;
        this.pictureRepository = pictureRepository;
    }

    @PostConstruct
    private void initCamera() {
        gpioHermanasController.initCamera(photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay, photoRotation);
    }


    public synchronized File takePicture() throws IOException {
        logger.info("taking a picture in root path {}.", rootPath);
        boolean lightIsAlreadySwitchedOn = switchLightOn();
        LocalDateTime localDateTime = LocalDateTime.now();
        String relativePath = generateRelativePath(localDateTime);
        File fileRoot = new File(rootPath + File.separator + relativePath);
        FileUtils.forceMkdir(fileRoot);
        String filename = generateFilename(localDateTime);
        File pictureFile = new File(fileRoot, filename);
        logger.info("Taking a picture now in {} ...", pictureFile.getAbsolutePath());
        try {
            gpioHermanasController.takePicture(new FilePictureCaptureHandler(pictureFile));
            logger.info("Save picture path in db.");
            pictureRepository.save(new Picture(relativePath + File.separator + filename));
            logger.info("... done.");
            return pictureFile;
        } catch (IOException e) {
            throw new IOException("Can't take picture or fetch file.", e);
        } finally {
            switchOffLight(lightIsAlreadySwitchedOn);
        }
    }

    private String generateRelativePath(LocalDateTime localDateTime) {
        return localDateTime.getYear() + "/" +
                localDateTime.getMonthValue() + "/" +
                localDateTime.getDayOfMonth();
    }

    private String generateFilename(LocalDateTime localDateTime) {
        return localDateTime.getYear() + "-" + localDateTime.getMonthValue() + "-" +
                localDateTime.getDayOfMonth() + "-" + localDateTime.getHour() + "-" + localDateTime.getMinute() + ".jpg";
    }

    /**
     * Switch off the light if the light was not already switched on by the current controller.
     * @param lightIsAlreadySwitchedOn true if the light was already on before.
     */
    private void switchOffLight(boolean lightIsAlreadySwitchedOn) {
        if (!lightIsAlreadySwitchedOn) {
            lightController.switchOff();
        } else {
            logger.debug("light was already switched on before taking picture, it must not be switched off.");
        }
    }

    /**
     * Switch on the light managing the previous state of the light.
     * @return true if this method has switched on the light.
     */
    private boolean switchLightOn() {
        boolean lightIsAlreadySwitchedOn = lightController.isSwitchedOn();
        if (!lightIsAlreadySwitchedOn) {
            lightController.switchOn();
        } else {
            logger.debug("light is already on, it's useless to switch it on again.");
        }
        return lightIsAlreadySwitchedOn;
    }

    /**
     * Takes a picture managing the IO exception.
     */
    public Optional<File> takePictureNoException() {
        try {
            return Optional.ofNullable(takePicture());
        } catch (IOException e) {
            logger.error("Can't take picture.", e);
            return Optional.empty();
        }
    }
}
