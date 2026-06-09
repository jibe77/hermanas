package org.jibe77.hermanas.client.ai;

/**
 * Carries a stable, machine-readable {@link #getCode()} alongside a
 * human-readable message. The REST controller maps the code to an HTTP status,
 * and the message is surfaced to the operator as-is.
 */
public class AiVisionException extends Exception {
    private final String code;

    public AiVisionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
