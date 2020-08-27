package org.jibe77.hermanas.gpio.camera;

import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.data.entity.Picture;
import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.jibe77.hermanas.gpio.light.LightController;
import org.jibe77.hermanas.gpio.light.LightIRController;
import org.jibe77.hermanas.gpio.light.LightSwitchedOnByCamera;
import org.jibe77.hermanas.scheduler.sun.SunTimeUtils;
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
    private LightIRController lightIRController;
    private SunTimeUtils sunTimeUtils;

    private LightSwitchedOnByCamera lightSwitchedOnByCamera = LightSwitchedOnByCamera.NONE;

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

    public CameraController(LightController lightController, LightIRController lightIRController, GpioHermanasController gpioHermanasController, PictureRepository pictureRepository, SunTimeUtils sunTimeUtils) {
        this.lightController = lightController;
        this.lightIRController = lightIRController;
        this.gpioHermanasController = gpioHermanasController;
        this.pictureRepository = pictureRepository;
        this.sunTimeUtils = sunTimeUtils;
    }

    @PostConstruct
    private void initCamera() {
        gpioHermanasController.initCamera(photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay, photoRotation);
    }


    public synchronized File takePicture() throws IOException {
        logger.info("taking a picture in root path {}.", rootPath);
        switchLightOn();
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
            switchOffLight();
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
     */
    private void switchOffLight() {
        switch (lightSwitchedOnByCamera) {
            case LIGHT :
                lightController.switchOff();
                break;
            case IR_LIGHT :
                lightIRController.switchOff();
                break;
            default:
                logger.debug("light hasn't been switched on before taking picture, it should not be switched off.");
                break;
        }
        lightSwitchedOnByCamera = LightSwitchedOnByCamera.NONE;
    }

    /**
     * Switch on the light managing the previous state of the light.
     */
    private void switchLightOn() {
        // by night, the IR Light is switched on
        if (sunTimeUtils.isDay()) {
            if (lightController.isSwitchedOn()) {
                logger.debug("light is already on, it's useless to switch it on again.");
                lightSwitchedOnByCamera = LightSwitchedOnByCamera.NONE;
            } else {
                logger.info("light has been switched on by camera.");
                lightController.switchOn();
                lightSwitchedOnByCamera = LightSwitchedOnByCamera.LIGHT;
            }
        } else {
            logger.info("IR Light has been switched on by camera.");
            lightIRController.switchOn();
            lightSwitchedOnByCamera = LightSwitchedOnByCamera.IR_LIGHT;
        }
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
