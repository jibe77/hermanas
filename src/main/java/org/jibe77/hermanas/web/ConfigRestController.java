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
import org.springframework.context.annotation.Lazy;

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
@Lazy
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration", description = "Admin endpoints for viewing and modifying system configuration")
@PreAuthorize("isAuthenticated()")
public class ConfigRestController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRestController.class);
    private final ConfigService configService;
    private final CacheManager cacheManager;
    private final org.jibe77.hermanas.client.ai.AiVisionCache aiVisionCache;
    private final org.jibe77.hermanas.service.camera.CameraService cameraService;
    private final org.jibe77.hermanas.service.music.MusicService musicService;

    public ConfigRestController(ConfigService configService,
                                CacheManager cacheManager,
                                org.jibe77.hermanas.client.ai.AiVisionCache aiVisionCache,
                                org.jibe77.hermanas.service.camera.CameraService cameraService,
                                org.jibe77.hermanas.service.music.MusicService musicService) {
        this.configService = configService;
        this.cacheManager = cacheManager;
        this.aiVisionCache = aiVisionCache;
        this.cameraService = cameraService;
        this.musicService = musicService;
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

        // Sun-time offsets + door force-schedule overrides
        Map<String, Object> sun = new LinkedHashMap<>();
        sun.put("light_on_minutes_before_sunset", configService.getLightOnTimeBeforeSunset());
        sun.put("door_close_minutes_after_sunset", configService.getDoorCloseTimeAfterSunset());
        sun.put("door_open_minutes_after_sunrise", configService.getDoorOpenTimeAfterSunrise());
        config.put("sun_offsets", sun);

        Map<String, Object> doorForce = new LinkedHashMap<>();
        doorForce.put("opening_enabled", configService.isDoorOpeningForceEnabled());
        doorForce.put("opening_time", configService.getDoorOpeningForceTime());
        doorForce.put("closing_enabled", configService.isDoorClosingForceEnabled());
        doorForce.put("closing_time", configService.getDoorClosingForceTime());
        config.put("door_force_schedule", doorForce);

        // Music volume — parse "N%" → int so the UI gets a clean number
        String volumeRaw = configService.getMusicVolumeRegular();
        int volumeInt = 0;
        if (volumeRaw != null) {
            try {
                volumeInt = Integer.parseInt(volumeRaw.replace("%", "").trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot parse music volume '{}', returning 0.", volumeRaw);
            }
        }
        Map<String, Object> musicSettings = new LinkedHashMap<>();
        musicSettings.put("volume_regular_percent", volumeInt);
        config.put("music_settings", musicSettings);

        // Servo calibration positions + durations
        Map<String, Integer> servo = new LinkedHashMap<>();
        servo.put("door_opening_position", configService.getDoorOpeningPosition());
        servo.put("door_closing_position", configService.getDoorClosingPosition());
        servo.put("door_opening_duration_ms", configService.getDoorOpeningDuration());
        servo.put("door_closing_duration_ms", configService.getDoorClosingDuration());
        config.put("servo_positions", servo);

        // Audio toggles
        Map<String, Boolean> audio = new LinkedHashMap<>();
        audio.put("cocorico_at_sunrise", configService.isCocoricoAtSunriseEnabled());
        audio.put("song_at_sunset", configService.isSongAtSunsetEnabled());
        config.put("audio_toggles", audio);

        // Notification toggles — email is opt-in per user, not a global flag.
        Map<String, Boolean> notifications = new LinkedHashMap<>();
        notifications.put("weather_enabled", configService.isWeatherInfoEnabled());
        config.put("notifications", notifications);

        // Camera image quality
        Map<String, Integer> camera = new LinkedHashMap<>();
        camera.put("brightness", configService.getCameraBrightness());
        camera.put("rotation", configService.getCameraRotation());
        camera.put("regular_quality", configService.getCameraRegularQuality());
        camera.put("high_quality", configService.getCameraHighQuality());
        config.put("camera_settings", camera);

        // Weather provider — return the URL template but mask the API key. We expose
        // only "set: true/false" + "length" so the admin can confirm the key is in
        // place without surfacing the secret to anyone with a session cookie.
        // Latitude/longitude are NOT returned here — they identify the physical
        // location of the chicken coop, which is sensitive data. The admin can
        // still write new values via PUT /api/v1/config/location/{latitude,longitude}
        // (write-only).
        Map<String, Object> weatherSettings = new LinkedHashMap<>();
        weatherSettings.put("url", configService.getWeatherInfoUrl());
        String key = configService.getWeatherInfoKey();
        weatherSettings.put("key_set", key != null && !key.trim().isEmpty()
                && !"to-override-in-application-properties-file".equals(key.trim()));
        weatherSettings.put("key_length", key == null ? 0 : key.trim().length());
        config.put("weather_settings", weatherSettings);

        // AI inference (URL + model of the local LLM used by /camera/analyze).
        Map<String, Object> aiSettings = new LinkedHashMap<>();
        aiSettings.put("inference_url", configService.getAiInferenceUrl());
        aiSettings.put("inference_model", configService.getAiInferenceModel());
        aiSettings.put("cache_ttl_ms", configService.getAiInferenceCacheTtlMs());
        aiSettings.put("prompt", configService.getAiInferencePrompt());
        aiSettings.put("prompt_default",
                org.jibe77.hermanas.client.ai.CameraPromptBuilder.DEFAULT_PROMPT);
        aiSettings.put("connect_timeout_ms", configService.getAiInferenceConnectTimeoutMs());
        aiSettings.put("read_timeout_ms", configService.getAiInferenceReadTimeoutMs());
        aiSettings.put("retry_max_attempts", configService.getAiInferenceRetryMaxAttempts());
        aiSettings.put("retry_initial_backoff_ms", configService.getAiInferenceRetryInitialBackoffMs());
        aiSettings.put("retry_max_backoff_ms", configService.getAiInferenceRetryMaxBackoffMs());
        config.put("ai_settings", aiSettings);

        // Email "from" address — recipients are derived from the user table at send time.
        Map<String, String> emailSettings = new LinkedHashMap<>();
        emailSettings.put("from", configService.getEmailNotificationFrom());
        config.put("email_settings", emailSettings);

        // SMTP transport — password never returned, only a "set" flag.
        Map<String, Object> smtpSettings = new LinkedHashMap<>();
        smtpSettings.put("host", configService.getMailHost());
        smtpSettings.put("port", configService.getMailPort());
        smtpSettings.put("username", configService.getMailUsername());
        smtpSettings.put("password_set", configService.isMailPasswordSet());
        smtpSettings.put("auth", configService.isMailSmtpAuth());
        smtpSettings.put("starttls", configService.isMailStartTlsEnable());
        config.put("email_smtp", smtpSettings);

        // Belt-and-braces no-cache headers: a reverse proxy in front of the Pi
        // (nginx / Cloudflare on hermanas.r3n4.uk) was caching this GET despite
        // Spring Security's defaults, so a PUT /config/camera/* persisted to DB
        // but the next GET /config served a stale JSON. The combination below is
        // the strictest CDN-friendly directive set:
        //   - no-store        : do not write to disk/memory
        //   - no-cache        : revalidate every time
        //   - max-age=0       : never reuse without revalidation
        //   - must-revalidate : do not serve stale on origin error
        //   - private         : not shareable across users
        //   - Pragma          : HTTP/1.0 proxies still in the wild
        //   - Vary: Cookie    : even non-compliant proxies will key per-session
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, max-age=0, must-revalidate, private")
                .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                .header(org.springframework.http.HttpHeaders.VARY,
                        org.springframework.http.HttpHeaders.COOKIE)
                .body(config);
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
        // Custom in-memory caches (not backed by Spring's CacheManager) are
        // wiped manually so a refresh button truly empties everything.
        aiVisionCache.clear();
        cachesCleared++;
        cameraService.clearPictureCache();
        cachesCleared++;

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

    // ─── Sun-time offsets ───────────────────────────────────────────────────────

    @Operation(summary = "Minutes the light turns on before sunset")
    @PutMapping("/sun/light-on-before-sunset")
    public ResponseEntity<String> setLightOnBeforeSunset(
            @Parameter(description = "Minutes (0 = exactly at sunset)", example = "15")
            @RequestParam long minutes) {
        configService.setLightOnTimeBeforeSunset(minutes);
        return ResponseEntity.ok("Light-on offset set to " + minutes + " minutes before sunset");
    }

    @Operation(summary = "Minutes the door closes after sunset")
    @PutMapping("/sun/door-close-after-sunset")
    public ResponseEntity<String> setDoorCloseAfterSunset(
            @Parameter(description = "Minutes (0 = exactly at sunset)", example = "45")
            @RequestParam long minutes) {
        configService.setDoorCloseTimeAfterSunset(minutes);
        return ResponseEntity.ok("Door close offset set to " + minutes + " minutes after sunset");
    }

    @Operation(summary = "Minutes the door opens after sunrise")
    @PutMapping("/sun/door-open-after-sunrise")
    public ResponseEntity<String> setDoorOpenAfterSunrise(
            @Parameter(description = "Minutes (0 = exactly at sunrise)", example = "0")
            @RequestParam long minutes) {
        configService.setDoorOpenTimeAfterSunrise(minutes);
        return ResponseEntity.ok("Door open offset set to " + minutes + " minutes after sunrise");
    }

    // ─── Music volume ───────────────────────────────────────────────────────────

    @Operation(summary = "Update music regular volume (0-100)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Volume updated successfully"),
            @ApiResponse(responseCode = "400", description = "Out of range (must be 0-100)")
    })
    @PutMapping("/music/volume")
    public ResponseEntity<String> setMusicVolume(
            @Parameter(description = "Volume percent (0-100)", example = "78")
            @RequestParam int percent) {
        configService.setMusicVolumeRegular(percent);
        // Appliqué tout de suite à la carte son : sans cela le réglage n'aurait
        // d'effet qu'à la lecture suivante, amixer n'étant invoqué qu'au démarrage
        // d'un morceau. Un échec côté matériel n'invalide pas la sauvegarde.
        musicService.applyConfiguredVolume();
        return ResponseEntity.ok("Music volume set to " + percent + "%");
    }

    // ─── Servo positions (calibration) ─────────────────────────────────────────

    @Operation(summary = "Update door opening servo position (1-100)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Position updated successfully"),
            @ApiResponse(responseCode = "400", description = "Out of range (must be 1-100)")
    })
    @PutMapping("/door/opening-position")
    public ResponseEntity<String> setDoorOpeningPosition(
            @Parameter(description = "Servo position 1-100", example = "16")
            @RequestParam int position) {
        configService.setDoorOpeningPosition(position);
        return ResponseEntity.ok("Door opening position set to " + position);
    }

    @Operation(summary = "Update door closing servo position (1-100)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Position updated successfully"),
            @ApiResponse(responseCode = "400", description = "Out of range (must be 1-100)")
    })
    @PutMapping("/door/closing-position")
    public ResponseEntity<String> setDoorClosingPosition(
            @Parameter(description = "Servo position 1-100", example = "5")
            @RequestParam int position) {
        configService.setDoorClosingPosition(position);
        return ResponseEntity.ok("Door closing position set to " + position);
    }

    @Operation(summary = "Update door opening duration (1-30000 ms)")
    @PutMapping("/door/opening-duration")
    public ResponseEntity<String> setDoorOpeningDuration(
            @Parameter(description = "Duration in milliseconds 1-30000", example = "10000")
            @RequestParam int durationMs) {
        configService.setDoorOpeningDuration(durationMs);
        return ResponseEntity.ok("Door opening duration set to " + durationMs + " ms");
    }

    @Operation(summary = "Update door closing duration (1-30000 ms)")
    @PutMapping("/door/closing-duration")
    public ResponseEntity<String> setDoorClosingDuration(
            @Parameter(description = "Duration in milliseconds 1-30000", example = "2350")
            @RequestParam int durationMs) {
        configService.setDoorClosingDuration(durationMs);
        return ResponseEntity.ok("Door closing duration set to " + durationMs + " ms");
    }

    // ─── Audio toggles ──────────────────────────────────────────────────────────

    @Operation(summary = "Enable/disable cocorico at sunrise")
    @PutMapping("/audio/cocorico-at-sunrise")
    public ResponseEntity<String> setCocoricoAtSunrise(@RequestParam boolean enabled) {
        configService.setCocoricoAtSunriseEnabled(enabled);
        return ResponseEntity.ok("Cocorico at sunrise " + (enabled ? "enabled" : "disabled"));
    }

    @Operation(summary = "Enable/disable song at sunset")
    @PutMapping("/audio/song-at-sunset")
    public ResponseEntity<String> setSongAtSunset(@RequestParam boolean enabled) {
        configService.setSongAtSunsetEnabled(enabled);
        return ResponseEntity.ok("Song at sunset " + (enabled ? "enabled" : "disabled"));
    }

    // ─── Notification toggles ───────────────────────────────────────────────────
    // No "email enabled" endpoint: email recipients are managed per-user.

    @Operation(summary = "Enable/disable weather info fetching")
    @PutMapping("/notifications/weather")
    public ResponseEntity<String> setWeatherInfo(@RequestParam boolean enabled) {
        configService.setWeatherInfoEnabled(enabled);
        return ResponseEntity.ok("Weather info " + (enabled ? "enabled" : "disabled"));
    }

    // ─── Door force-schedule overrides ─────────────────────────────────────────
    //
    // When enabled=true, the corresponding HH:mm value replaces the sunrise/sunset
    // computation for door open / close. Every derived timer (light-on, door
    // open/close protocol) shifts accordingly.

    @Operation(summary = "Enable/disable forced door opening time (overrides sunrise)")
    @PutMapping("/scheduler/door/opening-force/enabled")
    public ResponseEntity<String> setDoorOpeningForceEnabled(@RequestParam boolean enabled) {
        configService.setDoorOpeningForceEnabled(enabled);
        return ResponseEntity.ok("Door opening force " + (enabled ? "enabled" : "disabled"));
    }

    @Operation(summary = "Update forced door opening time (HH:mm)")
    @PutMapping("/scheduler/door/opening-force/time")
    public ResponseEntity<String> setDoorOpeningForceTime(@RequestParam String time) {
        configService.setDoorOpeningForceTime(time);
        return ResponseEntity.ok("Door opening force time set to " + time);
    }

    @Operation(summary = "Enable/disable forced door closing time (overrides sunset)")
    @PutMapping("/scheduler/door/closing-force/enabled")
    public ResponseEntity<String> setDoorClosingForceEnabled(@RequestParam boolean enabled) {
        configService.setDoorClosingForceEnabled(enabled);
        return ResponseEntity.ok("Door closing force " + (enabled ? "enabled" : "disabled"));
    }

    @Operation(summary = "Update forced door closing time (HH:mm)")
    @PutMapping("/scheduler/door/closing-force/time")
    public ResponseEntity<String> setDoorClosingForceTime(@RequestParam String time) {
        configService.setDoorClosingForceTime(time);
        return ResponseEntity.ok("Door closing force time set to " + time);
    }

    // ─── Camera (changes take effect after app reboot) ─────────────────────────

    @Operation(
            summary = "Update camera brightness (0-100)",
            description = "Takes effect on the next picture — picam configs are rebuilt at every shot."
    )
    @PutMapping("/camera/brightness")
    public ResponseEntity<String> setCameraBrightness(
            @Parameter(description = "Brightness 0-100", example = "60")
            @RequestParam int brightness) {
        configService.setCameraBrightness(brightness);
        cameraService.clearPictureCache();
        return ResponseEntity.ok("Camera brightness set to " + brightness);
    }

    @Operation(
            summary = "Update camera rotation (0/90/180/270)",
            description = "Takes effect on the next picture."
    )
    @PutMapping("/camera/rotation")
    public ResponseEntity<String> setCameraRotation(
            @Parameter(description = "Rotation 0/90/180/270", example = "180")
            @RequestParam int degrees) {
        configService.setCameraRotation(degrees);
        cameraService.clearPictureCache();
        return ResponseEntity.ok("Camera rotation set to " + degrees + "°");
    }

    @Operation(
            summary = "Update regular JPEG quality (1-100)",
            description = "Quality used by the dashboard snapshot (480×270). "
                        + "Lower values produce smaller files. Takes effect on the next picture."
    )
    @PutMapping("/camera/regular-quality")
    public ResponseEntity<String> setCameraRegularQuality(
            @Parameter(description = "JPEG quality 1-100", example = "45")
            @RequestParam int quality) {
        configService.setCameraRegularQuality(quality);
        cameraService.clearPictureCache();
        return ResponseEntity.ok("Camera regular quality set to " + quality);
    }

    @Operation(
            summary = "Update high JPEG quality (1-100)",
            description = "Quality used by the dedicated Webcam page (960×540). "
                        + "Takes effect on the next picture."
    )
    @PutMapping("/camera/high-quality")
    public ResponseEntity<String> setCameraHighQuality(
            @Parameter(description = "JPEG quality 1-100", example = "80")
            @RequestParam int quality) {
        configService.setCameraHighQuality(quality);
        cameraService.clearPictureCache();
        return ResponseEntity.ok("Camera high quality set to " + quality);
    }

    // ─── Weather provider settings ─────────────────────────────────────────────

    @Operation(summary = "Update OpenWeather API URL template")
    @PutMapping("/weather/url")
    public ResponseEntity<String> setWeatherUrl(@RequestParam String url) {
        configService.setWeatherInfoUrl(url);
        return ResponseEntity.ok("Weather URL updated");
    }

    @Operation(summary = "Update OpenWeather API key")
    @PutMapping("/weather/key")
    public ResponseEntity<String> setWeatherKey(@RequestParam String key) {
        configService.setWeatherInfoKey(key);
        return ResponseEntity.ok("Weather API key updated");
    }

    // ─── AI inference endpoint ─────────────────────────────────────────────────

    @Operation(summary = "Update the local LLM inference URL used by /camera/analyze")
    @PutMapping("/ai/inference-url")
    public ResponseEntity<String> setAiInferenceUrl(@RequestParam(required = false, defaultValue = "") String url) {
        configService.setAiInferenceUrl(url);
        // The cached analysis is tied to the previous URL — wiping it ensures the
        // next call really hits the new endpoint instead of returning stale text.
        aiVisionCache.clear();
        return ResponseEntity.ok("AI inference URL updated");
    }

    @Operation(summary = "Update the local LLM model name (OpenAI-compatible)")
    @PutMapping("/ai/inference-model")
    public ResponseEntity<String> setAiInferenceModel(@RequestParam(required = false, defaultValue = "") String model) {
        configService.setAiInferenceModel(model);
        aiVisionCache.clear();
        return ResponseEntity.ok("AI inference model updated");
    }

    @Operation(summary = "Update the AI vision cache TTL (in milliseconds). 0 disables the cache.")
    @PutMapping("/ai/cache-ttl-ms")
    public ResponseEntity<String> setAiInferenceCacheTtlMs(@RequestParam long ttlMs) {
        if (ttlMs < 0) {
            return ResponseEntity.badRequest().body("ttlMs must be >= 0");
        }
        configService.setAiInferenceCacheTtlMs(ttlMs);
        // Drop the existing entries so a stricter TTL (or 0 = disabled) is
        // honoured immediately instead of waiting for the old timestamps to expire.
        aiVisionCache.clear();
        return ResponseEntity.ok("AI inference cache TTL updated");
    }

    /**
     * Public read-only access to the built-in default prompt. Exposed so the
     * front-end demo mode (anonymous visitor) can pre-fill the AI prompt
     * textarea — every other field in the camera config panel stays gated
     * behind the class-level {@code @PreAuthorize("isAuthenticated()")},
     * but the default prompt itself is a hardcoded constant with no
     * secret value and is safe to surface to unauthenticated visitors.
     */
    @Operation(summary = "Returns the built-in default prompt used by the multimodal model. Public — no authentication required.")
    @GetMapping("/ai/prompt-default")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> getAiInferencePromptDefault() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("prompt_default",
                org.jibe77.hermanas.client.ai.CameraPromptBuilder.DEFAULT_PROMPT);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Update the prompt sent to the multimodal model. Empty string restores the built-in default.")
    @PutMapping("/ai/prompt")
    public ResponseEntity<String> setAiInferencePrompt(
            @RequestBody(required = false) String prompt) {
        // Log payload size + a short preview so a missed update can be triaged
        // from the journal — we do not want to dump multi-kB prompts into the
        // log on every save, hence the 60-char truncation.
        int len = prompt == null ? 0 : prompt.length();
        String preview = prompt == null ? "<null>"
                : (prompt.length() > 60 ? prompt.substring(0, 60) + "…" : prompt);
        logger.info("setAiInferencePrompt called: length={} preview='{}'",
                len, preview.replace("\n", " "));
        configService.setAiInferencePrompt(prompt);
        // Cached analyses were built with the previous prompt; drop them so the
        // next call uses the new one.
        aiVisionCache.clear();
        return ResponseEntity.ok("AI inference prompt updated");
    }

    // ─── AI inference timeouts & retry policy ──────────────────────────────────
    // These five values are read by AiVisionClient at construction time, so a
    // change here is persisted but only takes effect on the next reboot — same
    // contract as the camera settings.

    @Operation(summary = "Update the HTTP connect timeout (ms) for the inference call. Takes effect on next reboot.")
    @PutMapping("/ai/connect-timeout-ms")
    public ResponseEntity<String> setAiInferenceConnectTimeoutMs(@RequestParam int ms) {
        if (ms < 100 || ms > 600000) {
            return ResponseEntity.badRequest().body("ms must be between 100 and 600000");
        }
        configService.setAiInferenceConnectTimeoutMs(ms);
        return ResponseEntity.ok("AI inference connect timeout updated");
    }

    @Operation(summary = "Update the HTTP read timeout (ms) for the inference call. Takes effect on next reboot.")
    @PutMapping("/ai/read-timeout-ms")
    public ResponseEntity<String> setAiInferenceReadTimeoutMs(@RequestParam int ms) {
        if (ms < 1000 || ms > 1800000) {
            return ResponseEntity.badRequest().body("ms must be between 1000 and 1800000");
        }
        configService.setAiInferenceReadTimeoutMs(ms);
        return ResponseEntity.ok("AI inference read timeout updated");
    }

    @Operation(summary = "Update the total number of inference call attempts (initial + retries). Takes effect on next reboot.")
    @PutMapping("/ai/retry-max-attempts")
    public ResponseEntity<String> setAiInferenceRetryMaxAttempts(@RequestParam int attempts) {
        if (attempts < 1 || attempts > 10) {
            return ResponseEntity.badRequest().body("attempts must be between 1 and 10");
        }
        configService.setAiInferenceRetryMaxAttempts(attempts);
        return ResponseEntity.ok("AI inference retry attempts updated");
    }

    @Operation(summary = "Update the initial backoff between retries (ms). Takes effect on next reboot.")
    @PutMapping("/ai/retry-initial-backoff-ms")
    public ResponseEntity<String> setAiInferenceRetryInitialBackoffMs(@RequestParam long ms) {
        if (ms < 0 || ms > 60000) {
            return ResponseEntity.badRequest().body("ms must be between 0 and 60000");
        }
        configService.setAiInferenceRetryInitialBackoffMs(ms);
        return ResponseEntity.ok("AI inference retry initial backoff updated");
    }

    @Operation(summary = "Update the max backoff between retries (ms). Takes effect on next reboot.")
    @PutMapping("/ai/retry-max-backoff-ms")
    public ResponseEntity<String> setAiInferenceRetryMaxBackoffMs(@RequestParam long ms) {
        if (ms < 0 || ms > 120000) {
            return ResponseEntity.badRequest().body("ms must be between 0 and 120000");
        }
        configService.setAiInferenceRetryMaxBackoffMs(ms);
        return ResponseEntity.ok("AI inference retry max backoff updated");
    }

    // ─── GPS coordinates ───────────────────────────────────────────────────────
    // Used by both the sun-time scheduler (door open/close) and the weather lookup.

    @Operation(summary = "Update latitude (-90..90)")
    @PutMapping("/location/latitude")
    public ResponseEntity<String> setLatitude(@RequestParam double value) {
        configService.setLatitude(value);
        return ResponseEntity.ok("Latitude updated to " + value);
    }

    @Operation(summary = "Update longitude (-180..180)")
    @PutMapping("/location/longitude")
    public ResponseEntity<String> setLongitude(@RequestParam double value) {
        configService.setLongitude(value);
        return ResponseEntity.ok("Longitude updated to " + value);
    }

    // ─── Email "from" address ──────────────────────────────────────────────────
    // No "to" endpoint: notification recipients come from the user table.

    @Operation(summary = "Update email notification sender")
    @PutMapping("/email/from")
    public ResponseEntity<String> setEmailFrom(@RequestParam String email) {
        configService.setEmailNotificationFrom(email);
        return ResponseEntity.ok("Email 'from' updated to " + email);
    }

    // ─── SMTP transport ────────────────────────────────────────────────────────

    @Operation(summary = "Update SMTP host")
    @PutMapping("/mail/host")
    public ResponseEntity<String> setMailHost(@RequestParam String host) {
        configService.setMailHost(host);
        return ResponseEntity.ok("SMTP host updated to " + host);
    }

    @Operation(summary = "Update SMTP port (1..65535)")
    @PutMapping("/mail/port")
    public ResponseEntity<String> setMailPort(@RequestParam int port) {
        configService.setMailPort(port);
        return ResponseEntity.ok("SMTP port updated to " + port);
    }

    @Operation(summary = "Update SMTP username")
    @PutMapping("/mail/username")
    public ResponseEntity<String> setMailUsername(@RequestParam String username) {
        configService.setMailUsername(username);
        return ResponseEntity.ok("SMTP username updated");
    }

    @Operation(summary = "Update SMTP password (write-only; never returned)")
    @PutMapping("/mail/password")
    public ResponseEntity<String> setMailPassword(@RequestParam String password) {
        configService.setMailPassword(password);
        return ResponseEntity.ok("SMTP password updated");
    }

    @Operation(summary = "Toggle SMTP authentication")
    @PutMapping("/mail/auth")
    public ResponseEntity<String> setMailAuth(@RequestParam boolean enabled) {
        configService.setMailSmtpAuth(enabled);
        return ResponseEntity.ok("SMTP auth set to " + enabled);
    }

    @Operation(summary = "Toggle SMTP STARTTLS")
    @PutMapping("/mail/starttls")
    public ResponseEntity<String> setMailStartTls(@RequestParam boolean enabled) {
        configService.setMailStartTlsEnable(enabled);
        return ResponseEntity.ok("SMTP STARTTLS set to " + enabled);
    }

    // Removed:
    //   /system/<mode>/shutdown — the flag (machine.shutdown.*) was never read
    //     by any runtime code, only stored.
    //   /system/<mode>/wifi     — wifi.disabled.* is a hidden safety knob kept
    //     out of the admin UI on purpose (cutting wifi makes the chicken coop
    //     unreachable, including the admin doing the cutting). The seed value
    //     lives in application.properties.
}
