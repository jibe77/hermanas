package org.jibe77.hermanas.service.capture;

import java.util.Collections;
import java.util.List;

/**
 * Public view of a capture job, returned by the status endpoint. Stays minimal
 * on purpose so the polling loop on the SPA can decode it cheaply.
 *
 * <p>{@code message} carries the model's textual analysis when {@code status}
 * is {@link CaptureStatus#DONE}, {@code null} otherwise. {@code errorCode} and
 * {@code errorMessage} are populated only when {@code status} is
 * {@link CaptureStatus#ERROR}. {@code detections} carries the normalized
 * bounding boxes parsed from the model's hidden JSON tail — empty when the
 * model did not emit any (or emitted malformed coordinates).</p>
 */
public class CaptureStateDto {

    private final CaptureStatus status;
    private final String lang;
    private final String message;
    private final String errorCode;
    private final String errorMessage;
    private final boolean imageAvailable;
    private final List<DetectionDto> detections;

    public CaptureStateDto(CaptureStatus status, String lang, String message,
                           String errorCode, String errorMessage, boolean imageAvailable,
                           List<DetectionDto> detections) {
        this.status = status;
        this.lang = lang;
        this.message = message;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.imageAvailable = imageAvailable;
        this.detections = detections == null ? Collections.emptyList() : detections;
    }

    public CaptureStatus getStatus() { return status; }
    public String getLang() { return lang; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isImageAvailable() { return imageAvailable; }
    public List<DetectionDto> getDetections() { return detections; }
}
