package org.jibe77.hermanas.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.controller.door.DoorService;
import org.jibe77.hermanas.service.door.model.DoorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/door")
@Service
@Validated
@Tag(name = "Door", description = "Door control endpoints for opening, closing, and manual servo control")
public class DoorRestController {

    DoorService doorService;

    private static final Logger logger = LoggerFactory.getLogger(DoorRestController.class);

    public DoorRestController(DoorService doorService) {
        this.doorService = doorService;
    }

    @Operation(
            summary = "Close the door",
            description = "Closes the chicken coop door by activating the servo motor until the bottom button is pressed or timeout occurs"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Door successfully closed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Door not closed correctly (bottom button not pressed)",
                    content = @Content
            )
    })
    @PostMapping("/close")
    public boolean close(
            @Parameter(description = "Force closing even if door appears already closed", example = "false")
            @RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("closing door now  ...");
        doorService.closeDoorWithBottormButtonManagement(Boolean.parseBoolean(force));
        logger.info("... the door has been closed !");
        return true;
    }

    @Operation(
            summary = "Open the door",
            description = "Opens the chicken coop door by activating the servo motor until the top button is pressed or timeout occurs"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Door successfully opened",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            )
    })
    @PostMapping("/open")
    public boolean open(
            @Parameter(description = "Force opening even if door appears already open", example = "false")
            @RequestParam(defaultValue = "false", required = false) String force) {
        logger.info("opening door now  ...");
        boolean result = doorService.openDoorWithUpButtonManagment(Boolean.parseBoolean(force), false);
        logger.info("... done with result {} !", result);
        return result;
    }

    @Operation(
            summary = "Turn servo clockwise",
            description = "Manually turn the door servo motor clockwise for a specified duration (for testing/calibration)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Servo turned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid duration parameter",
                    content = @Content
            )
    })
    @GetMapping("/turnClockwise")
    public String turnClockwise(
            @Parameter(description = "Duration in milliseconds (1-30000)", example = "50")
            @RequestParam(defaultValue = "50", required = false)
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor clockwise  ...");
        doorService.turnServoClockwise(duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @Operation(
            summary = "Turn servo counter-clockwise",
            description = "Manually turn the door servo motor counter-clockwise for a specified duration (for testing/calibration)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Servo turned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid duration parameter",
                    content = @Content
            )
    })
    @GetMapping("/turnCounterClockwise")
    public String turnCounterClockwise(
            @Parameter(description = "Duration in milliseconds (1-30000)", example = "50")
            @RequestParam(defaultValue = "50", required = false)
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorService.turnServoCounterClockwise(duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @Operation(
            summary = "Turn servo with custom parameters",
            description = "Manually control the door servo motor with custom duty cycle, frequency, and duration (for advanced testing/calibration)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Servo turned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters",
                    content = @Content
            )
    })
    @GetMapping("/turnServo")
    public String turnServo(
            @Parameter(description = "PWM duty cycle percentage (0-100)", example = "50", required = true)
            @RequestParam
            @Min(value = 0, message = "Duty cycle must be between 0 and 100")
            @Max(value = 100, message = "Duty cycle must be between 0 and 100")
            int dutyCycle,
            @Parameter(description = "PWM frequency in Hz (1-1000)", example = "50", required = true)
            @RequestParam
            @Min(value = 1, message = "Frequency must be at least 1Hz")
            @Max(value = 1000, message = "Frequency cannot exceed 1000Hz")
            int frequency,
            @Parameter(description = "Duration in milliseconds (1-30000)", example = "1000", required = true)
            @RequestParam
            @Min(value = 1, message = "Duration must be at least 1ms")
            @Max(value = 30000, message = "Duration cannot exceed 30000ms")
            int duration) {
        logger.info("turning servomotor counter-clockwise  ...");
        doorService.turnServo(dutyCycle, frequency, duration);
        logger.info("... servomotor done !");
        return "done";
    }

    @Operation(
            summary = "Get door status",
            description = "Returns the current status of the door including position, button states, and operational information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Door status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DoorStatus.class))
            )
    })
    @GetMapping("/status")
    public DoorStatus statusInfo() {
        return doorService.statusInfo();
    }
}
