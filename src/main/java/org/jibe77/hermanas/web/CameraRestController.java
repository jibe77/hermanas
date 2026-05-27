package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.camera.PhotosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/camera")
@Tag(name = "Camera", description = "Camera control endpoints for taking pictures, streaming video, and monitoring door closing")
public class CameraRestController {

    CameraService cameraService;
    private final PhotosService photosService;

    public CameraRestController(CameraService cameraService, PhotosService photosService) {
        this.cameraService = cameraService;
        this.photosService = photosService;
    }

    private static final Logger logger = LoggerFactory.getLogger(CameraRestController.class);

    @Operation(
            summary = "Take a picture",
            description = "Captures a photo from the camera and returns it as a PNG image"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Picture captured successfully",
                    content = @Content(mediaType = "image/jpeg")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Camera error or I/O error",
                    content = @Content
            )
    })
    @GetMapping(value = "/takePicture", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] takePicture(
            @Parameter(description = "Use high quality settings for picture capture", example = "false")
            @RequestParam(defaultValue = "false") String highQuality) throws IOException, InterruptedException {
        File picture = cameraService.takePicture(Boolean.parseBoolean(highQuality));
        logger.info("return picture from {}.", picture.getAbsolutePath());
        try (FileInputStream fileInputStream = new FileInputStream(picture)) {
            return IOUtils.toByteArray(fileInputStream);
        }
    }

    @Operation(
            summary = "Start video stream",
            description = "Starts streaming video from the camera in MJPEG format"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Video stream started successfully",
                    content = @Content(mediaType = "multipart/x-mixed-replace")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Camera streaming error",
                    content = @Content
            )
    })
    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(final HttpServletResponse response) throws IOException {
        cameraService.stream();
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

    @Operation(
            summary = "Stop video stream",
            description = "Stops the currently active video stream from the camera"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Video stream stopped successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error stopping video stream",
                    content = @Content
            )
    })
    @GetMapping(value = "/stopStream")
    public void stopStream() throws InterruptedException, IOException {
        cameraService.stopStream();
    }

    @Operation(
            summary = "Get door closing rate",
            description = "Returns a percentage indicating how much the door is closed based on image analysis"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Closing rate retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Integer.class))
            )
    })
    @GetMapping("/closingRate")
    public int closingRate() {
        return cameraService.getClosingRate();
    }

    @Operation(
            summary = "List entries inside the photos directory",
            description = "Returns the directories and image files at the given relative path " +
                    "(omit `path` for the root). Only .jpg / .jpeg / .png files are returned; " +
                    "directory traversal is rejected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listing returned"),
            @ApiResponse(responseCode = "400", description = "Invalid path (traversal or non-directory)")
    })
    @GetMapping("/photos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listPhotos(
            @Parameter(description = "Relative path inside the photos directory, e.g. '2026/05'")
            @RequestParam(value = "path", required = false) String path) {
        try {
            return ResponseEntity.ok(photosService.list(path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            logger.warn("Failed to list photos at '{}'", path, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Download a picture from the photos archive",
            description = "Streams the requested image file (.jpg / .jpeg / .png) inside the " +
                    "photos directory. Authentication is required to keep the archive private."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image streamed",
                    content = @Content(mediaType = "image/*")),
            @ApiResponse(responseCode = "400", description = "Invalid path"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/photos/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> downloadPhoto(
            @Parameter(description = "Relative path of the picture inside the photos directory")
            @RequestParam("path") String path) {
        try {
            Path file = photosService.resolveImageFile(path);
            String name = file.getFileName().toString().toLowerCase();
            MediaType mediaType = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }
}
