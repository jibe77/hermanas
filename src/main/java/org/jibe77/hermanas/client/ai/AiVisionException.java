package org.jibe77.hermanas.client.ai;

/**
 * Carries a stable, machine-readable {@link #getCode()} alongside two messages:
 * a {@link #getPublicMessage() public} one safe to surface to the operator and
 * a {@link #getMessage() technical} one (the exception message) reserved for
 * server-side logging. Single-message constructor keeps backward compatibility:
 * the same text plays both roles when no public/technical split is required.
 */
public class AiVisionException extends Exception {
    private final String code;
    private final String publicMessage;

    public AiVisionException(String code, String message) {
        this(code, message, message);
    }

    public AiVisionException(String code, String technicalMessage, String publicMessage) {
        super(technicalMessage);
        this.code = code;
        this.publicMessage = publicMessage;
    }

    public String getCode() {
        return code;
    }

    public String getPublicMessage() {
        return publicMessage;
    }
}
