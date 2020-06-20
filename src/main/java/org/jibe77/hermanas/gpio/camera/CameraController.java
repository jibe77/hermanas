package org.jibe77.hermanas.gpio.camera;

import org.apache.commons.io.FileUtils;
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
import java.time.LocalDateTime;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;

@Component
@Scope("singleton")
public class CameraController {

    private CameraConfiguration config;

    @Value("${camera.path.root}")
    private String rootPath;

    Logger logger = LoggerFactory.getLogger(CameraController.class);

    @PostConstruct
    private void init() {
        logger.info("init camera config.");
         config = cameraConfiguration()
                .width(1920)
                .height(1080)
                .encoding(Encoding.JPEG)
                .quality(85)
                .delay(5000);
         takePicture();
    }

    public void takePicture() {
        logger.info("taking a picture.");
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
        } catch (Exception e) {
            logger.error("Error during picture due to ", e);
        }
    }
}
