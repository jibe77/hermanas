package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.service.energy.EnergyMode;
import org.jibe77.hermanas.service.energy.EnergyModeConfig;
import org.jibe77.hermanas.service.energy.EnergyModeEnum;
import org.jibe77.hermanas.service.energy.WifiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

@Lazy
@RestController
@RequestMapping("/api/v1/energy")
@Tag(name = "Energy", description = "Energy management endpoints for WiFi control and consumption modes")
public class EnergyRestController {

    WifiService wifiService;

    ConsumptionModeController consumptionModeController;

    private static final Logger logger = LoggerFactory.getLogger(EnergyRestController.class);

    public EnergyRestController(WifiService wifiService, ConsumptionModeController consumptionModeController) {
        this.wifiService = wifiService;
        this.consumptionModeController = consumptionModeController;
    }

    @Operation(
            summary = "Stop WiFi until next door event",
            description = "Disables WiFi after 3 seconds and keeps it off until the next door opening/closing event (for energy saving)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "WiFi shutdown scheduled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            )
    })
    @GetMapping(value = "/wifi/stopUntilNextDoorEvent")
    public boolean stopWifiUntilNextDoorEvent() {
        logger.info("The network wifi card is going to be disabled.");
        wifiService.turnOffAfter(3);
        return true;
    }

    @Operation(
            summary = "Enable/disable WiFi auto-switching",
            description = "Controls whether WiFi can be automatically switched off for energy saving"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "WiFi auto-switch setting updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            )
    })
    @GetMapping(value = "/wifi/wifiSwitchEnabled")
    public boolean wifiSwitchEnabled(
            @Parameter(description = "Enable WiFi auto-switching", required = true)
            boolean wifiSwitchEnabled) {
        wifiService.setWifiSwitchEnabled(wifiSwitchEnabled);
        return true;
    }

    @Operation(
            summary = "Get current energy mode",
            description = "Returns the active mode, whether ECO is forced, and the full month → mode mapping."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current energy mode retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnergyMode.class))
            )
    })
    @GetMapping(value = "/currentMode")
    public EnergyMode getEnergyMode() {
        return consumptionModeController.getCurrentEnergyMode();
    }

    @Operation(
            summary = "Get current energy mode configuration",
            description = "Returns the configuration for the currently active energy mode"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current config retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnergyModeConfig.class))
            )
    })
    @GetMapping(value = "/currentConfigMode")
    public EnergyModeConfig getCurrentConfigMode() {
        return consumptionModeController.getCurrentConfigMode();
    }

    @Operation(
            summary = "Get energy mode configuration",
            description = "Returns the configuration for a specific energy mode"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Config retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnergyModeConfig.class))
            )
    })
    @GetMapping(value = "/configMode")
    public EnergyModeConfig getEnergyConfigMode(
            @Parameter(description = "Energy mode name (e.g., ECO, SUNNY, REGULAR)", required = true)
            String energyMode) {
        return consumptionModeController.getEnergyModeConfig(energyMode);
    }

    @Operation(
            summary = "Update energy mode configuration",
            description = "Updates the configuration settings for an energy mode. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/updateMode")
    public void updateEnergyConfigMode(
            @Parameter(description = "Updated energy mode configuration", required = true)
            @RequestBody EnergyModeConfig energyModeConfig) {
        consumptionModeController.updateEnergyModeConfig(energyModeConfig);
    }

    @Operation(
            summary = "Update the monthly schedule",
            description = "Bulk-update the month → mode mapping (12 entries, keyed 1-12). Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monthly mapping updated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/monthlyMapping")
    public void updateMonthlyMapping(
            @Parameter(description = "Map of month number (1-12) → EnergyModeEnum", required = true)
            @RequestBody Map<Integer, EnergyModeEnum> mapping) {
        consumptionModeController.updateMonthlyMapping(mapping);
    }

    @Operation(
            summary = "Force or release the ECO mode",
            description = "When true, ECO mode is used regardless of the monthly schedule. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Force flag updated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/forceEco")
    public void setEcoForced(
            @Parameter(description = "true to force ECO, false to use the monthly schedule", required = true)
            @RequestParam("forced") boolean forced) {
        consumptionModeController.setEcoForced(forced);
    }
}
