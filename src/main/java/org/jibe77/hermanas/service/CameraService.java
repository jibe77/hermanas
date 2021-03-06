package org.jibe77.hermanas.service;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public @ResponseBody byte[] takePicture(@RequestParam(defaultValue = "false") String highQuality) throws IOException, InterruptedException {
        File picture = cameraController.takePicture(Boolean.parseBoolean(highQuality));
        logger.info("return picture from {}.", picture.getAbsolutePath());
        try (FileInputStream fileInputStream = new FileInputStream(picture)) {
            return IOUtils.toByteArray(fileInputStream);
        }
    }

    @GetMapping(value = "/camera/stream", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(final HttpServletResponse response) throws IOException {
        cameraController.stream();
        logger.info("stream has been called in camera controller, wait 500 milli-seconds ....");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.info("Interrupted sleep.");
            Thread.currentThread().interrupt();
        }
        response.setContentType("multipart/x-mixed-replace;boundary=boundarydonotcross");
        response.setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0");
        logger.info("content and header set");
        StreamingResponseBody streamingResponseBody = outputStream -> {
            URL streamUrl = new URL("http://localhost:8081/?action=stream");
            BufferedInputStream bufferedInputStream = new BufferedInputStream(streamUrl.openStream());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            try {
                int size;
                byte[] data = new byte[4096];
                logger.info("let's start copying the stream ...");
                do {
                    size = bufferedInputStream.read(data);
                    if (size != -1) {
                        bufferedOutputStream.write(data, 0, size);
                        bufferedOutputStream.flush();
                    }
                } while (size != -1);
                logger.info("done, close outputstream.");
                outputStream.close();
            } catch (ClientAbortException e) {
                try {
                    logger.info("Client connection aborted, closing stream.");
                    outputStream.close();
                    stopStream();
                } catch (InterruptedException ex) {
                    logger.error("Interrupted stop stream !", e);
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
            }
        };
        return new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
    }

    @GetMapping(value = "/camera/stopStream")
    public void stopStream() throws InterruptedException, IOException {
        cameraController.stopStream();
    }

    @GetMapping("/camera/closingRate")
    public int closingRate() {
        return cameraController.getClosingRate();
    }
}
