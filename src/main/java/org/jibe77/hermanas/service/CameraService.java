package org.jibe77.hermanas.service;

import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

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

    @GetMapping(value = "/camera/stream", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(final HttpServletResponse response) throws IOException, InterruptedException {
        cameraController.stream();
        logger.info("stream has been called in camera controller, wait 2 seconds ....");
        wait(2000);

        response.setContentType("multipart/x-mixed-replace;boundary=boundarydonotcross");
        response.setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0");
        logger.info("content and header set");
        StreamingResponseBody streamingResponseBody = outputStream -> {
            URL streamUrl = new URL("http://localhost:8081/?action=stream");
            BufferedInputStream bufferedInputStream = new BufferedInputStream(streamUrl.openStream());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            int size;
            byte[] data = new byte[1024];
            logger.info("let's start copying the stream ...");
            do {

                size = bufferedInputStream.read(data);
                if (size != -1) {
                    bufferedOutputStream.write(data, 0 , size);
                    bufferedOutputStream.flush();
                    logger.info("writing into stream array of size {}.", size);
                }
            } while (size != -1);
            logger.info("done, close outputstream.");
            outputStream.close();

        };
        return new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
    }

    @GetMapping(value = "/camera/stopStream")
    public void stopStream() throws InterruptedException, IOException {
        cameraController.stopStream();
    }
}
