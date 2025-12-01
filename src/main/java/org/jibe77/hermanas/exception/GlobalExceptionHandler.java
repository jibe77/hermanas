package org.jibe77.hermanas.exception;

import org.jibe77.hermanas.image.PredictionException;
import org.jibe77.hermanas.security.ratelimit.RateLimitExceededException;
import org.jibe77.hermanas.service.door.DoorNotClosedCorrectlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors for @Valid annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Validation Failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errors.put("fieldErrors", fieldErrors);

        logger.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handle constraint violations (e.g., @NotNull, @Min, @Max on method parameters).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Constraint Violation");
        errors.put("message", ex.getMessage());

        logger.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handle type mismatch errors (e.g., passing string when integer expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Type Mismatch");
        errors.put("message", String.format("Parameter '%s' should be of type %s",
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"));

        logger.warn("Type mismatch error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handle IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Invalid Argument");
        errors.put("message", ex.getMessage());

        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handle NumberFormatException (e.g., invalid string to integer conversion).
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleNumberFormatException(NumberFormatException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Invalid Number Format");
        errors.put("message", "Invalid number format in request parameter");

        logger.warn("Number format error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handle DoorNotClosedCorrectlyException - indicates door automation failure.
     */
    @ExceptionHandler(DoorNotClosedCorrectlyException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<Map<String, Object>> handleDoorNotClosedCorrectlyException(DoorNotClosedCorrectlyException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errors.put("error", "Door Operation Failed");
        errors.put("message", "Door did not close correctly. Please check door mechanism and sensors.");

        logger.error("Door failed to close correctly", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
    }

    /**
     * Handle PredictionException - indicates image analysis failure.
     */
    @ExceptionHandler(PredictionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handlePredictionException(PredictionException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errors.put("error", "Image Prediction Failed");
        errors.put("message", "Failed to analyze door image");
        if (ex.getDoorStatus() != null) {
            errors.put("doorStatus", ex.getDoorStatus());
        }

        logger.error("Image prediction failed with door status: {}", ex.getDoorStatus(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

    /**
     * Handle RateLimitExceededException - indicates too many requests from a client.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceededException(RateLimitExceededException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errors.put("error", "Rate Limit Exceeded");
        errors.put("message", ex.getMessage());

        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errors);
    }

    /**
     * Handle all other uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errors.put("error", "Internal Server Error");
        errors.put("message", "An unexpected error occurred");

        logger.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }
}
