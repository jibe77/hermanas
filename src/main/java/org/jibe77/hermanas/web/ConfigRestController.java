package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for configuration management.
 * All endpoints require USER role authentication (admin-only access).
 *
 * <p>Allows viewing and modifying runtime configuration values stored in the database.
 * Values fall back to application.properties defaults if not overridden in database.</p>
 *
 * @see org.jibe77.hermanas.service.config.ConfigService
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration", description = "Admin endpoints for viewing and modifying system configuration")
@PreAuthorize("hasRole('USER')")  // All endpoints in this controller require authentication
public class ConfigRestController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRestController.class);
    private final ConfigService configService;
    private final CacheManager cacheManager;

    public ConfigRestController(ConfigService configService, CacheManager cacheManager) {
        this.configService = configService;
        this.cacheManager = cacheManager;
    }

    // ============================================================================
    // GET Endpoints - View Configuration
    // ============================================================================

    /**
     * Gets all current configuration values (database-overridden or defaults).
     * Useful for displaying in a configuration UI panel.
     *
     * @return map of all configuration values organized by category
     */
    @Operation(
            summary = "Get all configuration values",
            description = "Returns all configuration values including timers, consumption modes, and system behavior settings. " +
                         "Values shown are either database-overridden or defaults from application.properties."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration values retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfig() {
        logger.info("Admin viewing all configuration values");

        Map<String, Object> config = new LinkedHashMap<>();

        // Light timers
        Map<String, Long> light = new LinkedHashMap<>();
        light.put("eco_delay_ms", configService.getLightSecurityTimerDelayEco());
        light.put("regular_delay_ms", configService.getLightSecurityTimerDelayRegular());
        light.put("sunny_delay_ms", configService.getLightSecurityTimerDelaySunny());
        config.put("light_timers", light);

        // Fan timers
        Map<String, Long> fan = new LinkedHashMap<>();
        fan.put("eco_delay_ms", configService.getFanSecurityTimerDelayEco());
        fan.put("regular_delay_ms", configService.getFanSecurityTimerDelayRegular());
        fan.put("sunny_delay_ms", configService.getFanSecurityTimerDelaySunny());
        config.put("fan_timers", fan);

        // Music timers
        Map<String, Long> music = new LinkedHashMap<>();
        music.put("eco_delay_ms", configService.getMusicSecurityTimerDelayEco());
        music.put("regular_delay_ms", configService.getMusicSecurityTimerDelayRegular());
        music.put("sunny_delay_ms", configService.getMusicSecurityTimerDelaySunny());
        config.put("music_timers", music);

        // Consumption mode settings
        Map<String, Object> consumption = new LinkedHashMap<>();
        Map<Integer, String> monthly = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthly.put(m, configService.getMonthMode(m).name());
        }
        consumption.put("monthly_mapping", monthly);
        consumption.put("eco_mode_forced", configService.isConsumptionModeEcoForce());
        config.put("consumption_mode", consumption);

        // System behavior block intentionally omitted: the only flags it used to expose
        // (machine.shutdown.*, wifi.disabled.*) are either unused or hidden safety knobs
        // that should not surface in the admin UI.

        return ResponseEntity.ok(config);
    }

    /**
     * Refreshes all configuration caches.
     *
     * <p>This endpoint evicts all configuration caches, forcing fresh reads
     * from the database on the next configuration access. Useful for hot-reloading
     * configuration changes made directly to the database without restarting the application.</p>
     *
     * <p><strong>Use cases:</strong></p>
     * <ul>
     *   <li>Manual database updates outside of the REST API</li>
     *   <li>Forcing a refresh after external configuration changes</li>
     *   <li>Troubleshooting cached configuration values</li>
     * </ul>
     *
     * @return success message with number of caches cleared
     */
    @Operation(
            summary = "Refresh all configuration caches",
            description = "Evicts all configuration caches to force fresh reads from database. " +
                         "Enables hot-reload of configuration changes without application restart."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Caches refreshed successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCaches() {
        logger.info("Admin requested configuration cache refresh");

        int cachesCleared = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
                cachesCleared++;
                logger.debug("Cleared cache: {}", cacheName);
            }
        }

        logger.info("Configuration cache refresh completed. {} caches cleared", cachesCleared);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Configuration caches refreshed successfully");
        response.put("caches_cleared", cachesCleared);
        response.put("hot_reload_enabled", true);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // PUT Endpoints - Update Configuration
    // ============================================================================

    /**
     * Updates light security timer delay for eco mode.
     *
     * @param delayMs timer delay in milliseconds (must be >= 0)
     * @return success response
     */
    @Operation(summary = "Update light timer for eco mode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid value (e.g., negative)")
    })
    @PutMapping("/light/eco")
    public ResponseEntity<String> setLightEco(
            @Parameter(description = "Timer delay in milliseconds", example = "300000")
            @RequestParam long delayMs) {
        configService.setLightSecurityTimerDelayEco(delayMs);
        return ResponseEntity.ok("Light eco timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update light timer for regular mode")
    @PutMapping("/light/regular")
    public ResponseEntity<String> setLightRegular(@RequestParam long delayMs) {
        configService.setLightSecurityTimerDelayRegular(delayMs);
        return ResponseEntity.ok("Light regular timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update light timer for sunny mode")
    @PutMapping("/light/sunny")
    public ResponseEntity<String> setLightSunny(@RequestParam long delayMs) {
        configService.setLightSecurityTimerDelaySunny(delayMs);
        return ResponseEntity.ok("Light sunny timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update fan timer for eco mode")
    @PutMapping("/fan/eco")
    public ResponseEntity<String> setFanEco(@RequestParam long delayMs) {
        configService.setFanSecurityTimerDelayEco(delayMs);
        return ResponseEntity.ok("Fan eco timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update fan timer for regular mode")
    @PutMapping("/fan/regular")
    public ResponseEntity<String> setFanRegular(@RequestParam long delayMs) {
        configService.setFanSecurityTimerDelayRegular(delayMs);
        return ResponseEntity.ok("Fan regular timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update fan timer for sunny mode")
    @PutMapping("/fan/sunny")
    public ResponseEntity<String> setFanSunny(@RequestParam long delayMs) {
        configService.setFanSecurityTimerDelaySunny(delayMs);
        return ResponseEntity.ok("Fan sunny timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update music timer for eco mode")
    @PutMapping("/music/eco")
    public ResponseEntity<String> setMusicEco(@RequestParam long delayMs) {
        configService.setMusicSecurityTimerDelayEco(delayMs);
        return ResponseEntity.ok("Music eco timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update music timer for regular mode")
    @PutMapping("/music/regular")
    public ResponseEntity<String> setMusicRegular(@RequestParam long delayMs) {
        configService.setMusicSecurityTimerDelayRegular(delayMs);
        return ResponseEntity.ok("Music regular timer updated to " + delayMs + " ms");
    }

    @Operation(summary = "Update music timer for sunny mode")
    @PutMapping("/music/sunny")
    public ResponseEntity<String> setMusicSunny(@RequestParam long delayMs) {
        configService.setMusicSecurityTimerDelaySunny(delayMs);
        return ResponseEntity.ok("Music sunny timer updated to " + delayMs + " ms");
    }

    // Solstice-based endpoints were removed when the consumption mode was migrated
    // to a configurable month → mode mapping. Use PUT /api/v1/energy/monthlyMapping
    // to edit the schedule.

    @Operation(
            summary = "Force eco mode on/off",
            description = "When true, eco mode is forced regardless of season"
    )
    @PutMapping("/consumption/force-eco")
    public ResponseEntity<String> setForceEco(@RequestParam boolean force) {
        configService.setConsumptionModeEcoForce(force);
        return ResponseEntity.ok("Eco mode force set to " + force);
    }

    // Removed:
    //   /system/<mode>/shutdown — the flag (machine.shutdown.*) was never read
    //     by any runtime code, only stored.
    //   /system/<mode>/wifi     — wifi.disabled.* is a hidden safety knob kept
    //     out of the admin UI on purpose (cutting wifi makes the chicken coop
    //     unreachable, including the admin doing the cutting). The seed value
    //     lives in application.properties.
}
