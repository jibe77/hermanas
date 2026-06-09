package org.jibe77.hermanas.service.capture;

/**
 * Public view of a capture job, returned by the status endpoint. Stays minimal
 * on purpose so the polling loop on the SPA can decode it cheaply.
 *
 * <p>{@code message} carries the model's textual analysis when {@code status}
 * is {@link CaptureStatus#DONE}, {@code null} otherwise. {@code errorCode} and
 * {@code errorMessage} are populated only when {@code status} is
 * {@link CaptureStatus#ERROR}.</p>
 */
public class CaptureStateDto {

    private final CaptureStatus status;
    private final String lang;
    private final String message;
    private final String errorCode;
    private final String errorMessage;
    private final boolean imageAvailable;

    public CaptureStateDto(CaptureStatus status, String lang, String message,
                           String errorCode, String errorMessage, boolean imageAvailable) {
        this.status = status;
        this.lang = lang;
        this.message = message;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.imageAvailable = imageAvailable;
    }

    public CaptureStatus getStatus() { return status; }
    public String getLang() { return lang; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isImageAvailable() { return imageAvailable; }
}
