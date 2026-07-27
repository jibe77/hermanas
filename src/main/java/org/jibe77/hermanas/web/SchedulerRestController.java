package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.scheduler.sun.model.NextEvents;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Lazy;

import java.time.format.DateTimeFormatter;

@Lazy
@RestController
@RequestMapping("/api/v1/scheduler")
@Tag(name = "Scheduler", description = "Scheduled events endpoints based on sunrise/sunset times")
public class SchedulerRestController {

    SunTimeManager sunTimeManager;

    public SchedulerRestController(SunTimeManager sunTimeManager,
                            ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
    }

    @Operation(
            summary = "Get next door closing time",
            description = "Returns the time when the door will automatically close (based on sunset)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Door closing time retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "18:30"))
            )
    })
    @GetMapping(value = "/doorClosingTime")
    public ResponseEntity<String> getNextDoorClosingTime() {
        return noCache(sunTimeManager.getNextDoorClosingTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM)));
    }

    @Operation(
            summary = "Get next door opening time",
            description = "Returns the time when the door will automatically open (based on sunrise)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Door opening time retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "08:15"))
            )
    })
    @GetMapping(value = "/doorOpeningTime")
    public ResponseEntity<String> getNextDoorOpeningTime() {
        return noCache(sunTimeManager.getNextDoorOpeningTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM)));
    }

    @Operation(
            summary = "Get next light on time",
            description = "Returns the time when the light will automatically turn on (before sunset)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Light on time retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "17:28"))
            )
    })
    @GetMapping(value = "/lightOnTime")
    public ResponseEntity<String> getNextLightOnTime() {
        return noCache(sunTimeManager.getNextLightOnTime().format(DateTimeFormatter.ofPattern(SunTimeManager.HH_MM)));
    }

    @Operation(
            summary = "Get all next scheduled events",
            description = "Returns all upcoming scheduled events (door opening/closing, light on). Example: {\"nextDoorOpeningTime\":\"2021-01-31T08:14:47\",\"nextLightOnTime\":\"2021-01-30T17:28:49\",\"nextDoorClosingTime\":\"2021-01-30T17:58:49\"}"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Next events retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NextEvents.class))
            )
    })
    @GetMapping(value = "/nextEvents")
    public ResponseEntity<NextEvents> getNextEvents() {
        return noCache(sunTimeManager.getNextEvents());
    }

    // Belt-and-braces no-cache headers so Safari / the Angular service worker /
    // any reverse proxy in front of the Pi does not serve a stale schedule after
    // the operator flipped the force-open/close toggle. Same set of directives
    // as ConfigRestController.getAllConfig — see the rationale over there.
    private static <T> ResponseEntity<T> noCache(T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.VARY, HttpHeaders.COOKIE)
                .body(body);
    }
}
