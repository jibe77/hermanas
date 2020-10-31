package org.jibe77.hermanas.service;

import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
public class CameraService {

    CameraController cameraController;

    public CameraService(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    Logger logger = LoggerFactory.getLogger(CameraService.class);

    @GetMapping(value = "/camera/takePicture", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] takePicture() throws IOException {
        File picture = cameraController.takePicture(false);
        logger.info("return picture from {}.", picture.getAbsolutePath());
        try (FileInputStream fileInputStream = new FileInputStream(picture)) {
            return IOUtils.toByteArray(fileInputStream);
        }
    }

    @GetMapping(value = "/camera/stream")
    public @ResponseBody String stream() throws IOException {
        cameraController.stream();
        return "<html>\n" +
                "    <body>\n" +
                "        <img src=\"http://poulailler.local:8081/?action=stream\">\n" +
                "    </body>\n" +
                "</html>";
    }

    @GetMapping(value = "/camera/stopStream")
    public void stopStream() {
        cameraController.stopStream();
    }
}
