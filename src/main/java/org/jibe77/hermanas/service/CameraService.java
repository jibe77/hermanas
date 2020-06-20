package org.jibe77.hermanas.service;

import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.gpio.camera.CameraController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class CameraService {

    CameraController cameraController;

    public CameraService(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    @GetMapping(value = "/camera/takePicture")
    public @ResponseBody byte[] takePicture() throws IOException {
        File picture = cameraController.takePicture();
        InputStream in = getClass().getResourceAsStream(picture.getAbsolutePath());
        return IOUtils.toByteArray(in);
    }
}
