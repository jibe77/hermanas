package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.light.LightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/light")
@Tag(name = "Light", description = "Light control endpoints for switching and status monitoring")
public class LightRestController {

    LightService lightService;
    EventService eventService;

    public LightRestController(LightService lightService, EventService eventService) {
        this.lightService = lightService;
        this.eventService = eventService;
    }

    @Operation(
            summary = "Switch light on/off",
            description = "Turns the coop light on or off"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Light switched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @PostMapping(value = "/switch", produces = "application/json")
    public Status switcher(
            @Parameter(description = "True to turn light on, false to turn off", required = true)
            boolean param) {
        Status status = lightService.switcher(param);
        // Record on actual reported state (e.g. switcher() might no-op on identical state).
        eventService.record(status.getStatusEnum() == StatusEnum.ON
                ? EventType.LIGHT_ON : EventType.LIGHT_OFF);
        return status;
    }

    @Operation(
            summary = "Get light status",
            description = "Returns the current status of the light (on/off)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Light status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            )
    })
    @GetMapping(value = "/status")
    public Status getStatus() {
        return lightService.getStatus();
    }
}
