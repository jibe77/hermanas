package org.jibe77.hermanas.gpio.camera;

import com.pi4j.wiringpi.Gpio;
import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.data.entity.Picture;
import org.jibe77.hermanas.data.repository.PictureRepository;
import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.jibe77.hermanas.gpio.light.LightIRController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.co.caprica.picam.CaptureFailedException;
import uk.co.caprica.picam.FilePictureCaptureHandler;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Scope("singleton")
public class CameraController {

    private LightIRController lightIRController;

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

    @Value("${camera.path.root}")
    private String rootPath;

    Logger logger = LoggerFactory.getLogger(CameraController.class);

    public CameraController(LightIRController lightIRController, GpioHermanasController gpioHermanasController, PictureRepository pictureRepository) {
        this.lightIRController = lightIRController;
        this.gpioHermanasController = gpioHermanasController;
        this.pictureRepository = pictureRepository;
    }

    @PostConstruct
    private void initCamera() {
        gpioHermanasController.initCamera(photoWidth, photoHeight, photoEncoding, photoQuality, photoDelay);
    }


    public synchronized File takePicture() throws IOException {
        logger.info("taking a picture in root path {}.", rootPath);
        lightIRController.switchOn();
            LocalDateTime localDateTime = LocalDateTime.now();
            String relativePath =  "/" +
                    localDateTime.getYear() + "/" +
                    localDateTime.getMonthValue() + "/" +
                    localDateTime.getDayOfMonth();
            File fileRoot = new File(
                    rootPath + relativePath);
            FileUtils.forceMkdir(fileRoot);
            String filename = localDateTime.getYear() + "-" + localDateTime.getMonthValue() + "-" +
                    localDateTime.getDayOfMonth() + "-" + localDateTime.getHour() + "-" + localDateTime.getMinute() + ".jpg";
            File pictureFile = new File(fileRoot, filename);
            logger.info("Taking a picture now in {} ...", pictureFile.getAbsolutePath());
        try {
            gpioHermanasController.takePicture(new FilePictureCaptureHandler(pictureFile));
            logger.info("Save picture path in db.");
            pictureRepository.save(new Picture(relativePath + "/" + filename));
            logger.info("... done.");
            return pictureFile;
        } catch (IOException e) {
            throw new IOException("Can't take picture or fetch file.", e);
        } finally {
            lightIRController.switchOff();
        }
    }

    public void takePictureNoException() {
        try {
            takePicture();
        } catch (IOException e) {
            logger.error("Can't take picture.", e);
        }
    }
}
