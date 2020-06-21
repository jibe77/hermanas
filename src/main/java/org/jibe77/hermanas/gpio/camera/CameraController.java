package org.jibe77.hermanas.gpio.camera;

import org.apache.commons.io.FileUtils;
import org.jibe77.hermanas.gpio.light.LightIRController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.co.caprica.picam.Camera;
import uk.co.caprica.picam.CameraConfiguration;
import uk.co.caprica.picam.FilePictureCaptureHandler;
import uk.co.caprica.picam.enums.Encoding;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;

@Component
@Scope("singleton")
public class CameraController {

    private CameraConfiguration config;

    private LightIRController lightIRController;

    @Value("${camera.path.root}")
    private String rootPath;

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

    Logger logger = LoggerFactory.getLogger(CameraController.class);

    public CameraController(LightIRController lightIRController) {
        this.lightIRController = lightIRController;
    }

    @PostConstruct
    private void init() {
        logger.info("init camera config.");
         config = cameraConfiguration()
                .width(photoWidth)
                .height(photoHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(photoQuality)
                .delay(photoDelay);
    }

    public synchronized File takePicture() throws IOException {
        logger.info("taking a picture.");
        lightIRController.switchOn();
        try(Camera camera = new Camera(config)) {
            LocalDateTime localDateTime = LocalDateTime.now();
            File fileRoot = new File(
                    rootPath + "/" +
                            localDateTime.getYear() + "/" +
                            localDateTime.getMonthValue() + "/" +
                            localDateTime.getDayOfMonth());
            FileUtils.forceMkdir(fileRoot);
            String filename = localDateTime.getYear() + "-" + localDateTime.getMonthValue() + "-" +
                    localDateTime.getDayOfMonth() + "-" + localDateTime.getHour() + "-" + localDateTime.getMinute() + ".jpg";
            File pictureFile = new File(fileRoot, filename);
            logger.info("Taking a picture now ...");
            camera.takePicture(new FilePictureCaptureHandler(pictureFile));
            logger.info("... done.");
            return pictureFile;
        } catch (Exception e) {
            logger.error("Error during picture due to ", e);
            throw new IOException("Can't take picture or fetch file.", e);
        } finally {
            lightIRController.switchOff();
        }
    }

    public void takePictureNoException() {
        try {
            takePicture();
        } catch (IOException e) {
            logger.error("Can't take picture.");
        }
    }
}
