package org.jibe77.hermanas.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.controller.fan.FanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fan")
@Tag(name = "Fan", description = "Fan control endpoints for switching and status monitoring")
public class FanRestController {

    FanService fanService;

    public FanRestController(FanService fanService) {
        this.fanService = fanService;
    }

    @Operation(
            summary = "Switch fan on/off",
            description = "Turns the ventilation fan on or off"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Fan switched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @GetMapping(value = "/switch", produces = "application/json")
    public Status switcher(
            @Parameter(description = "True to turn fan on, false to turn off", required = true)
            boolean param) {
        return fanService.switcher(param);
    }

    @Operation(
            summary = "Get fan status",
            description = "Returns the current status of the fan (on/off)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Fan status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @GetMapping(value = "/status")
    public Status getStatus() {
        return fanService.getStatus();
    }
}
