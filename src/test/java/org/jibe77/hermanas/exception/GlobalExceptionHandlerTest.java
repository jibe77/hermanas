package org.jibe77.hermanas.exception;

import org.jibe77.hermanas.image.DoorStatus;
import org.jibe77.hermanas.image.PredictionException;
import org.jibe77.hermanas.service.door.DoorNotClosedCorrectlyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequest() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgumentException(ex);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
        assertEquals("Invalid Argument", response.getBody().get("error"));
        assertEquals("Invalid argument provided", response.getBody().get("message"));
    }

    @Test
    void handleNumberFormatException_shouldReturnBadRequest() {
        // Given
        NumberFormatException ex = new NumberFormatException("Invalid number");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleNumberFormatException(ex);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
        assertEquals("Invalid Number Format", response.getBody().get("error"));
        assertEquals("Invalid number format in request parameter", response.getBody().get("message"));
    }

    @Test
    void handleDoorNotClosedCorrectlyException_shouldReturnUnprocessableEntity() {
        // Given
        DoorNotClosedCorrectlyException ex = new DoorNotClosedCorrectlyException();

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDoorNotClosedCorrectlyException(ex);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getBody().get("status"));
        assertEquals("Door Operation Failed", response.getBody().get("error"));
        assertEquals("Door did not close correctly. Please check door mechanism and sensors.",
            response.getBody().get("message"));
    }

    @Test
    void handlePredictionException_shouldReturnInternalServerError() {
        // Given
        DoorStatus doorStatus = DoorStatus.CLOSED;
        PredictionException ex = new PredictionException(doorStatus);

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePredictionException(ex);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().get("status"));
        assertEquals("Image Prediction Failed", response.getBody().get("error"));
        assertEquals("Failed to analyze door image", response.getBody().get("message"));
        assertEquals(doorStatus, response.getBody().get("doorStatus"));
    }

    @Test
    void handleGlobalException_shouldReturnInternalServerError() {
        // Given
        Exception ex = new Exception("Unexpected error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGlobalException(ex);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
