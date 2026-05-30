package org.jibe77.hermanas.service.config;

import org.apache.commons.lang3.StringUtils;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.metrics.HermanasMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Configuration service implementing a tiered configuration pattern:
 * - Primary source: Database (runtime-configurable via REST API)
 * - Fallback source: application.properties (version-controlled defaults)
 *
 * <p>This allows runtime reconfiguration without redeployment while maintaining
 * safe defaults. Particularly useful for IoT devices like Hermanas where
 * seasonal adjustments and remote tuning are needed.</p>
 *
 * <p><strong>Architecture:</strong></p>
 * <ul>
 *   <li>All getters check database first, then fall back to @Value properties</li>
 *   <li>All setters persist to database and invalidate Spring cache</li>
 *   <li>Spring @Cacheable minimizes database queries (critical for Pi Zero)</li>
 *   <li>Error handling prevents invalid data from breaking the system</li>
 *   <li>Validation ensures only valid values can be persisted</li>
 * </ul>
 *
 * @see org.jibe77.hermanas.data.entity.Parameter
 * @see org.jibe77.hermanas.data.repository.ParameterRepository
 */
@Component
public class ConfigService {

    @Value("${light.security.timer.delay.eco}")
    private long lightSecurityTimerDelayEco;

    @Value("${light.security.timer.delay.regular}")
    private long lightSecurityTimerDelayRegular;

    @Value("${light.security.timer.delay.sunny}")
    private long lightSecurityTimerDelaySunny;

    @Value("${fan.security.timer.delay.eco}")
    private long fanSecurityTimerDelayEco;

    @Value("${fan.security.timer.delay.regular}")
    private long fanSecurityTimerDelayRegular;

    @Value("${fan.security.timer.delay.sunny}")
    private long fanSecurityTimerDelaySunny;

    @Value("${music.security.timer.delay.eco}")
    private long musicSecurityTimerDelayEco;

    @Value("${music.security.timer.delay.regular}")
    private long musicSecurityTimerDelayRegular;

    @Value("${music.security.timer.delay.sunny}")
    private long musicSecurityTimerDelaySunny;

    @Value("${consumption.mode.eco.force}")
    private boolean consumptionModeEcoForce;

    @Value("${wifi.disabled.eco}")
    boolean wifiDisabledInEcoMode;

    @Value("${wifi.disabled.sunny}")
    boolean wifiDisabledInSunnyMode;

    @Value("${wifi.disabled.regular}")
    boolean wifiDisabledInRegularMode;

    @Value("${music.playlist.selected:}")
    String selectedPlaylist;

    // ─── Sun-time offsets (minutes) ──────────────────────────────────────────────
    // Three values that drift seasonally: when to open the door after sunrise, when
    // to close it after sunset, and when to turn the light on before sunset. Stored
    // in DB so the user can tune them without redeploying when daylight length changes.

    @Value("${suntime.scheduler.light.on.time_before_sunset}")
    private long lightOnTimeBeforeSunset;

    @Value("${suntime.scheduler.door.close.time_after_sunset}")
    private long doorCloseTimeAfterSunset;

    @Value("${suntime.scheduler.door.open.time_after_sunrise}")
    private long doorOpenTimeAfterSunrise;

    // ─── Music volume (regular level, 0-100 percent) ──────────────────────────────

    @Value("${music.volume.regular}")
    private String musicVolumeRegular;

    // ─── Servo motor positions (calibration) ──────────────────────────────────────
    // These are the duty-cycle "positions" sent to the servo to define what counts
    // as "fully open" and "fully closed". Each new coop installation drifts by a
    // few units because of cable tension and the servo's own end-stop tolerance,
    // so they are persisted in DB to let an admin recalibrate in place.

    @Value("${door.opening.position}")
    private int doorOpeningPosition;

    @Value("${door.closing.position}")
    private int doorClosingPosition;

    @Value("${door.opening.duration}")
    private int doorOpeningDuration;

    @Value("${door.closing.duration}")
    private int doorClosingDuration;

    // ─── Audio toggles ────────────────────────────────────────────────────────────

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    @Value("${play.song.at.sunset}")
    private boolean songAtSunsetEnabled;

    // ─── Notification toggles ────────────────────────────────────────────────────

    @Value("${email.notification.enabled}")
    private boolean emailNotificationEnabled;

    @Value("${weather.info.enabled}")
    private boolean weatherInfoEnabled;

    // ─── Sun schedule ────────────────────────────────────────────────────────────

    @Value("${suntime.sunrise.force_at_8}")
    private boolean sunriseForceAt8;

    // ─── Camera image quality ─────────────────────────────────────────────────────

    @Value("${camera.brightness}")
    private int cameraBrightness;

    @Value("${camera.rotation}")
    private int cameraRotation;

    // ─── Weather provider settings ────────────────────────────────────────────────

    @Value("${weather.info.url}")
    private String weatherInfoUrl;

    @Value("${weather.info.key}")
    private String weatherInfoKey;

    // ─── Email recipient / sender ─────────────────────────────────────────────────

    @Value("${email.notification.to}")
    private String emailNotificationTo;

    @Value("${email.notification.from}")
    private String emailNotificationFrom;

    ParameterRepository parameterRepository;

    @Autowired(required = false)
    HermanasMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    public ConfigService(ParameterRepository parameterRepository, @Autowired(required = false) HermanasMetrics metrics) {
        this.parameterRepository = parameterRepository;
        this.metrics = metrics;
    }

    // ============================================================================
    // Generic Configuration Helper Methods
    // ============================================================================

    /**
     * Generic configuration getter with database-first fallback pattern.
     *
     * <p><strong>Configuration Priority:</strong></p>
     * <ol>
     *   <li>Database value (if present and valid)</li>
     *   <li>Properties file default (fallback)</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Invalid database values (e.g., "abc" for Long) → logs warning, uses default</li>
     *   <li>Database connection errors → logs error, uses default</li>
     *   <li>System remains functional even with corrupted config data</li>
     * </ul>
     *
     * @param <T> the configuration value type
     * @param key the configuration key (e.g., "light.security.timer.delay.eco")
     * @param defaultValue the fallback value from application.properties
     * @param parser function to convert String to type T (e.g., Long::parseLong)
     * @return the configuration value (database or default)
     */
    private <T> T getConfigValue(String key, T defaultValue, Function<String, T> parser) {
        try {
            Parameter parameter = parameterRepository.findByEntryKey(key);
            if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
                try {
                    T parsedValue = parser.apply(parameter.getEntryValue());
                    logger.debug("Config '{}' loaded from database: {}", key, parsedValue);
                    return parsedValue;
                } catch (IllegalArgumentException e) {
                    // Covers NumberFormatException (subclass of IllegalArgumentException)
                    logger.warn("Invalid database value for '{}': '{}'. Using default: {}. Error: {}",
                               key, parameter.getEntryValue(), defaultValue, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error reading config '{}' from database. Using default: {}. Error: {}",
                        key, defaultValue, e.getMessage(), e);
        }

        logger.debug("Config '{}' using default value: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Generic configuration setter with validation and audit logging.
     *
     * <p>Persists the value to database and evicts the cache entry to ensure
     * fresh values on next read.</p>
     *
     * <p><strong>Validation:</strong> If a validator is provided, the value is
     * checked before persistence. Invalid values cause an IllegalArgumentException.</p>
     *
     * @param key the configuration key
     * @param value the value to persist
     * @param validator optional validation function that returns error message (null if valid)
     * @throws IllegalArgumentException if validator rejects the value
     */
    private void setConfigValue(String key, Object value, Function<Object, String> validator) {
        // Validate if validator provided
        if (validator != null) {
            String validationError = validator.apply(value);
            if (validationError != null) {
                throw new IllegalArgumentException("Invalid value for '" + key + "': " + validationError);
            }
        }

        Parameter parameter = new Parameter();
        parameter.setEntryKey(key);
        parameter.setEntryValue(String.valueOf(value));

        logger.info("Saving config to database: {} = {}", key, value);
        parameterRepository.save(parameter);

        // Record configuration change metric (if metrics available)
        if (metrics != null) {
            metrics.recordConfigChange(key);
        }
    }

    // ============================================================================
    // Validation Helpers
    // ============================================================================

    /**
     * Validator for non-negative long values (timer delays, etc.).
     * Prevents negative timer values which would be nonsensical.
     *
     * @return validator function that returns error message if invalid, null if valid
     */
    private Function<Object, String> nonNegativeLongValidator() {
        return value -> {
            long longValue = (long) value;
            if (longValue < 0) {
                return "Timer delay cannot be negative. Got: " + longValue;
            }
            return null; // valid
        };
    }

    /**
     * Validator for positive integer values (day counts, etc.).
     * Ensures values like "days around solstice" are meaningful (> 0).
     *
     * @return validator function that returns error message if invalid, null if valid
     */
    private Function<Object, String> positiveIntValidator() {
        return value -> {
            int intValue = (int) value;
            if (intValue <= 0) {
                return "Value must be positive. Got: " + intValue;
            }
            return null; // valid
        };
    }

    // ============================================================================
    // Light Configuration Methods
    // ============================================================================

    /**
     * Gets the light security timer delay for eco mode (milliseconds).
     * This determines how long the light stays on before automatic shutdown in eco mode.
     *
     * @return timer delay in milliseconds (database value or default from properties)
     */
    @Cacheable(value = {"lightSecurityTimerDelayEco"})
    public long getLightSecurityTimerDelayEco() {
        return getConfigValue("light.security.timer.delay.eco", lightSecurityTimerDelayEco, Long::parseLong);
    }

    /**
     * Sets the light security timer delay for eco mode.
     * Value must be non-negative.
     *
     * @param lightSecurityTimerDelayEco timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "lightSecurityTimerDelayEco")
    public void setLightSecurityTimerDelayEco(long lightSecurityTimerDelayEco) {
        setConfigValue("light.security.timer.delay.eco", lightSecurityTimerDelayEco, nonNegativeLongValidator());
    }

    /**
     * Gets the light security timer delay for regular mode (milliseconds).
     *
     * @return timer delay in milliseconds (database value or default from properties)
     */
    @Cacheable(value = {"lightSecurityTimerDelayRegular"})
    public long getLightSecurityTimerDelayRegular() {
        return getConfigValue("light.security.timer.delay.regular", lightSecurityTimerDelayRegular, Long::parseLong);
    }

    /**
     * Sets the light security timer delay for regular mode.
     * Value must be non-negative.
     *
     * @param lightSecurityTimerDelayRegular timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "lightSecurityTimerDelayRegular")
    public void setLightSecurityTimerDelayRegular(long lightSecurityTimerDelayRegular) {
        setConfigValue("light.security.timer.delay.regular", lightSecurityTimerDelayRegular, nonNegativeLongValidator());
    }

    /**
     * Gets the light security timer delay for sunny mode (milliseconds).
     *
     * @return timer delay in milliseconds (database value or default from properties)
     */
    @Cacheable(value = {"lightSecurityTimerDelaySunny"})
    public long getLightSecurityTimerDelaySunny() {
        return getConfigValue("light.security.timer.delay.sunny", lightSecurityTimerDelaySunny, Long::parseLong);
    }

    /**
     * Sets the light security timer delay for sunny mode.
     * Value must be non-negative.
     *
     * @param lightSecurityTimerDelaySunny timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "lightSecurityTimerDelaySunny")
    public void setLightSecurityTimerDelaySunny(long lightSecurityTimerDelaySunny) {
        setConfigValue("light.security.timer.delay.sunny", lightSecurityTimerDelaySunny, nonNegativeLongValidator());
    }

    // ============================================================================
    // Fan Configuration Methods
    // ============================================================================

    /**
     * Gets the fan security timer delay for eco mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"fanSecurityTimerDelayEco"})
    public long getFanSecurityTimerDelayEco() {
        return getConfigValue("fan.security.timer.delay.eco", fanSecurityTimerDelayEco, Long::parseLong);
    }

    /**
     * Sets the fan security timer delay for eco mode.
     *
     * @param fanSecurityTimerDelayEco timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "fanSecurityTimerDelayEco")
    public void setFanSecurityTimerDelayEco(long fanSecurityTimerDelayEco) {
        setConfigValue("fan.security.timer.delay.eco", fanSecurityTimerDelayEco, nonNegativeLongValidator());
    }

    /**
     * Gets the fan security timer delay for regular mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"fanSecurityTimerDelayRegular"})
    public long getFanSecurityTimerDelayRegular() {
        return getConfigValue("fan.security.timer.delay.regular", fanSecurityTimerDelayRegular, Long::parseLong);
    }

    /**
     * Sets the fan security timer delay for regular mode.
     *
     * @param fanSecurityTimerDelayRegular timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "fanSecurityTimerDelayRegular")
    public void setFanSecurityTimerDelayRegular(long fanSecurityTimerDelayRegular) {
        setConfigValue("fan.security.timer.delay.regular", fanSecurityTimerDelayRegular, nonNegativeLongValidator());
    }

    /**
     * Gets the fan security timer delay for sunny mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"fanSecurityTimerDelaySunny"})
    public long getFanSecurityTimerDelaySunny() {
        return getConfigValue("fan.security.timer.delay.sunny", fanSecurityTimerDelaySunny, Long::parseLong);
    }

    /**
     * Sets the fan security timer delay for sunny mode.
     *
     * @param fanSecurityTimerDelaySunny timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "fanSecurityTimerDelaySunny")
    public void setFanSecurityTimerDelaySunny(long fanSecurityTimerDelaySunny) {
        setConfigValue("fan.security.timer.delay.sunny", fanSecurityTimerDelaySunny, nonNegativeLongValidator());
    }

    // ============================================================================
    // Music Configuration Methods
    // ============================================================================

    /**
     * Gets the music security timer delay for eco mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"musicSecurityTimerDelayEco"})
    public long getMusicSecurityTimerDelayEco() {
        return getConfigValue("music.security.timer.delay.eco", musicSecurityTimerDelayEco, Long::parseLong);
    }

    /**
     * Sets the music security timer delay for eco mode.
     *
     * @param musicSecurityTimerDelayEco timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "musicSecurityTimerDelayEco")
    public void setMusicSecurityTimerDelayEco(long musicSecurityTimerDelayEco) {
        setConfigValue("music.security.timer.delay.eco", musicSecurityTimerDelayEco, nonNegativeLongValidator());
    }

    /**
     * Gets the music security timer delay for regular mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"musicSecurityTimerDelayRegular"})
    public long getMusicSecurityTimerDelayRegular() {
        return getConfigValue("music.security.timer.delay.regular", musicSecurityTimerDelayRegular, Long::parseLong);
    }

    /**
     * Sets the music security timer delay for regular mode.
     *
     * @param musicSecurityTimerDelayRegular timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "musicSecurityTimerDelayRegular")
    public void setMusicSecurityTimerDelayRegular(long musicSecurityTimerDelayRegular) {
        setConfigValue("music.security.timer.delay.regular", musicSecurityTimerDelayRegular, nonNegativeLongValidator());
    }

    /**
     * Gets the music security timer delay for sunny mode (milliseconds).
     *
     * @return timer delay in milliseconds
     */
    @Cacheable(value = {"musicSecurityTimerDelaySunny"})
    public long getMusicSecurityTimerDelaySunny() {
        return getConfigValue("music.security.timer.delay.sunny", musicSecurityTimerDelaySunny, Long::parseLong);
    }

    /**
     * Sets the music security timer delay for sunny mode.
     *
     * @param musicSecurityTimerDelaySunny timer delay in milliseconds
     * @throws IllegalArgumentException if value is negative
     */
    @CacheEvict(value = "musicSecurityTimerDelaySunny")
    public void setMusicSecurityTimerDelaySunny(long musicSecurityTimerDelaySunny) {
        setConfigValue("music.security.timer.delay.sunny", musicSecurityTimerDelaySunny, nonNegativeLongValidator());
    }

    // ============================================================================
    // Consumption Mode Configuration Methods
    // ============================================================================

    /**
     * Returns the energy mode assigned to a given month for the automatic schedule.
     * The mapping is editable at runtime via {@link #setMonthMode}; the seeds come
     * from {@code consumption.mode.month.<n>} in application.properties.
     *
     * @param month month number 1-12 (1 = January)
     * @return the {@link org.jibe77.hermanas.service.energy.EnergyModeEnum} for that month
     */
    public org.jibe77.hermanas.service.energy.EnergyModeEnum getMonthMode(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12, got " + month);
        }
        String key = "consumption.mode.month." + month;
        String fallback = monthDefault(month);
        String raw = getConfigValue(key, fallback, Function.identity());
        try {
            return org.jibe77.hermanas.service.energy.EnergyModeEnum.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid monthly mode '{}' for month {}, falling back to {}.", raw, month, fallback);
            return org.jibe77.hermanas.service.energy.EnergyModeEnum.valueOf(fallback);
        }
    }

    /**
     * Updates the energy mode for a given month.
     */
    public void setMonthMode(int month, org.jibe77.hermanas.service.energy.EnergyModeEnum mode) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12, got " + month);
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        setConfigValue("consumption.mode.month." + month, mode.name(), null);
    }

    /**
     * Seed values for the monthly schedule, used when no override is stored in DB
     * and no @Value-injected property is available (i.e. in the tests that build a
     * ConfigService by hand).
     */
    private String monthDefault(int month) {
        switch (month) {
            case 3:
            case 10:
                return "REGULAR";
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return "SUNNY";
            default:
                return "ECO";
        }
    }

    /**
     * Checks if eco mode is forced (overrides seasonal logic).
     *
     * @return true if eco mode should be forced regardless of season
     */
    @Cacheable(value = {"consumptionModeEcoForce"})
    public boolean isConsumptionModeEcoForce() {
        return getConfigValue("consumption.mode.eco.force", consumptionModeEcoForce, Boolean::valueOf);
    }

    /**
     * Sets whether eco mode should be forced.
     *
     * @param consumptionModeEcoForce true to force eco mode
     */
    @CacheEvict(value = "consumptionModeEcoForce")
    public void setConsumptionModeEcoForce(boolean consumptionModeEcoForce) {
        this.consumptionModeEcoForce = consumptionModeEcoForce;
        setConfigValue("consumption.mode.eco.force", consumptionModeEcoForce, null);
    }

    // ============================================================================
    // System Behavior Configuration (Shutdown/WiFi per Mode)
    // ============================================================================

    /**
     * Checks if WiFi should be disabled in eco mode.
     *
     * @return true if WiFi should be disabled
     */
    @Cacheable(value = {"wifiDisabledInEcoMode"})
    public boolean isWifiDisabledInEcoMode() {
        return getConfigValue("wifi.disabled.eco", wifiDisabledInEcoMode, Boolean::valueOf);
    }

    /**
     * Sets whether WiFi should be disabled in eco mode.
     *
     * @param wifiDisabledInEcoMode true to disable WiFi
     */
    @CacheEvict(value = "wifiDisabledInEcoMode")
    public void setWifiDisabledInEcoMode(boolean wifiDisabledInEcoMode) {
        setConfigValue("wifi.disabled.eco", wifiDisabledInEcoMode, null);
    }

    /**
     * Checks if WiFi should be disabled in sunny mode.
     *
     * @return true if WiFi should be disabled
     */
    @Cacheable(value = {"wifiDisabledInSunnyMode"})
    public boolean isWifiDisabledInSunnyMode() {
        return getConfigValue("wifi.disabled.sunny", wifiDisabledInSunnyMode, Boolean::valueOf);
    }

    /**
     * Sets whether WiFi should be disabled in sunny mode.
     *
     * @param wifiDisabledInSunnyMode true to disable WiFi
     */
    @CacheEvict(value = "wifiDisabledInSunnyMode")
    public void setWifiDisabledInSunnyMode(boolean wifiDisabledInSunnyMode) {
        setConfigValue("wifi.disabled.sunny", wifiDisabledInSunnyMode, null);
    }

    /**
     * Checks if WiFi should be disabled in regular mode.
     *
     * @return true if WiFi should be disabled
     */
    @Cacheable(value = {"wifiDisabledInRegularMode"})
    public boolean isWifiDisabledInRegularMode() {
        return getConfigValue("wifi.disabled.regular", wifiDisabledInRegularMode, Boolean::valueOf);
    }

    /**
     * Sets whether WiFi should be disabled in regular mode.
     * Fixed typo: parameter name now matches method intent.
     *
     * @param wifiDisabledInRegularMode true to disable WiFi
     */
    @CacheEvict(value = "wifiDisabledInRegularMode")
    public void setWifiDisabledInRegularMode(boolean wifiDisabledInRegularMode) {
        setConfigValue("wifi.disabled.regular", wifiDisabledInRegularMode, null);
    }

    // ============================================================================
    // Music Playlist Configuration
    // ============================================================================

    /**
     * Gets the currently selected music playlist (sub-directory name of music.path.mix).
     * Empty string means "no specific playlist" — caller can fall back to all songs.
     *
     * @return playlist name or empty string
     */
    @Cacheable(value = {"selectedPlaylist"})
    public String getSelectedPlaylist() {
        return getConfigValue("music.playlist.selected", selectedPlaylist == null ? "" : selectedPlaylist, s -> s);
    }

    /**
     * Sets the currently selected playlist. The value is the sub-directory name
     * inside music.path.mix (e.g. "classic"). Empty string means "no selection".
     * The MusicService is responsible for validating that the directory exists.
     *
     * @param playlist sub-directory name, or empty/null for "no selection"
     */
    @CacheEvict(value = "selectedPlaylist")
    public void setSelectedPlaylist(String playlist) {
        setConfigValue("music.playlist.selected", playlist == null ? "" : playlist, null);
    }

    // ============================================================================
    // Sun-time Offset Configuration
    // ============================================================================
    //
    // Each setter evicts BOTH its own cache AND the matching SunTimeManager cache
    // ("light-on" / "door-opening" / "door-closing") so the next call to
    // getNextLightOnTime() etc. picks up the new offset on the very next tick.

    /**
     * Minutes to turn the light on before sunset. 0 means "exactly at sunset".
     *
     * @return minutes (database value or default from properties)
     */
    @Cacheable(value = "lightOnTimeBeforeSunset")
    public long getLightOnTimeBeforeSunset() {
        return getConfigValue("suntime.scheduler.light.on.time_before_sunset",
                lightOnTimeBeforeSunset, Long::parseLong);
    }

    /**
     * Sets the offset (minutes) at which the light is switched on before sunset.
     *
     * @param minutes non-negative number of minutes
     * @throws IllegalArgumentException if minutes is negative
     */
    @CacheEvict(value = {"lightOnTimeBeforeSunset", "light-on"}, allEntries = true)
    public void setLightOnTimeBeforeSunset(long minutes) {
        setConfigValue("suntime.scheduler.light.on.time_before_sunset", minutes,
                nonNegativeLongValidator());
    }

    /**
     * Minutes after sunset at which the door is closed. Allows giving the chickens a
     * grace period to wander back in before sealing the coop.
     *
     * @return minutes (database value or default from properties)
     */
    @Cacheable(value = "doorCloseTimeAfterSunset")
    public long getDoorCloseTimeAfterSunset() {
        return getConfigValue("suntime.scheduler.door.close.time_after_sunset",
                doorCloseTimeAfterSunset, Long::parseLong);
    }

    /**
     * Sets the door close offset (minutes) after sunset.
     */
    @CacheEvict(value = {"doorCloseTimeAfterSunset", "door-closing"}, allEntries = true)
    public void setDoorCloseTimeAfterSunset(long minutes) {
        setConfigValue("suntime.scheduler.door.close.time_after_sunset", minutes,
                nonNegativeLongValidator());
    }

    /**
     * Minutes after sunrise at which the door is opened. Lets the user delay the
     * opening if the area is colder than expected at first light.
     *
     * @return minutes (database value or default from properties)
     */
    @Cacheable(value = "doorOpenTimeAfterSunrise")
    public long getDoorOpenTimeAfterSunrise() {
        return getConfigValue("suntime.scheduler.door.open.time_after_sunrise",
                doorOpenTimeAfterSunrise, Long::parseLong);
    }

    /**
     * Sets the door open offset (minutes) after sunrise.
     */
    @CacheEvict(value = {"doorOpenTimeAfterSunrise", "door-opening"}, allEntries = true)
    public void setDoorOpenTimeAfterSunrise(long minutes) {
        setConfigValue("suntime.scheduler.door.open.time_after_sunrise", minutes,
                nonNegativeLongValidator());
    }

    // ============================================================================
    // Music Volume
    // ============================================================================

    /**
     * Regular music playback volume as a percent string (e.g. "78%"), the format
     * {@code amixer} expects. The frontend handles the int ↔ "N%" conversion.
     *
     * @return volume string with trailing percent sign
     */
    @Cacheable(value = "musicVolumeRegular")
    public String getMusicVolumeRegular() {
        return getConfigValue("music.volume.regular", musicVolumeRegular, s -> s);
    }

    /**
     * Updates the regular music volume. Accepts an integer percent 0-100 and
     * persists it as "{n}%" so existing call sites keep working unchanged.
     *
     * @param percent 0-100
     * @throws IllegalArgumentException if percent is out of bounds
     */
    @CacheEvict(value = "musicVolumeRegular")
    public void setMusicVolumeRegular(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Volume must be between 0 and 100, got " + percent);
        }
        setConfigValue("music.volume.regular", percent + "%", null);
    }

    // ============================================================================
    // Servo Motor Calibration
    // ============================================================================
    //
    // The two positions below are tuned to the physical chicken-coop install. They
    // are NOT general-purpose and depend on the cable tension / servo wear. The
    // setters validate aggressively (1..100, matching the servo's GPIO range) so
    // a typo can't drive the motor into its end-stop. Setting these is admin-only
    // at the REST layer.

    @Cacheable(value = "doorOpeningPosition")
    public int getDoorOpeningPosition() {
        return getConfigValue("door.opening.position", doorOpeningPosition, Integer::parseInt);
    }

    @CacheEvict(value = "doorOpeningPosition")
    public void setDoorOpeningPosition(int position) {
        if (position < 1 || position > 100) {
            throw new IllegalArgumentException("Servo position must be 1..100, got " + position);
        }
        setConfigValue("door.opening.position", position, null);
    }

    @Cacheable(value = "doorClosingPosition")
    public int getDoorClosingPosition() {
        return getConfigValue("door.closing.position", doorClosingPosition, Integer::parseInt);
    }

    @CacheEvict(value = "doorClosingPosition")
    public void setDoorClosingPosition(int position) {
        if (position < 1 || position > 100) {
            throw new IllegalArgumentException("Servo position must be 1..100, got " + position);
        }
        setConfigValue("door.closing.position", position, null);
    }

    // ============================================================================
    // Door Opening/Closing Duration (ms)
    // ============================================================================
    //
    // Wall-clock time the servo is driven for. Tuning higher gives a slower but
    // gentler movement; tuning lower risks the door not fully reaching its
    // physical end-stop. 1..30000 ms validation matches the servo controller's
    // safety guard.

    @Cacheable(value = "doorOpeningDuration")
    public int getDoorOpeningDuration() {
        return getConfigValue("door.opening.duration", doorOpeningDuration, Integer::parseInt);
    }

    @CacheEvict(value = "doorOpeningDuration")
    public void setDoorOpeningDuration(int durationMs) {
        if (durationMs < 1 || durationMs > 30000) {
            throw new IllegalArgumentException("Duration must be 1..30000 ms, got " + durationMs);
        }
        setConfigValue("door.opening.duration", durationMs, null);
    }

    @Cacheable(value = "doorClosingDuration")
    public int getDoorClosingDuration() {
        return getConfigValue("door.closing.duration", doorClosingDuration, Integer::parseInt);
    }

    @CacheEvict(value = "doorClosingDuration")
    public void setDoorClosingDuration(int durationMs) {
        if (durationMs < 1 || durationMs > 30000) {
            throw new IllegalArgumentException("Duration must be 1..30000 ms, got " + durationMs);
        }
        setConfigValue("door.closing.duration", durationMs, null);
    }

    // ============================================================================
    // Audio Toggles
    // ============================================================================

    @Cacheable(value = "cocoricoAtSunriseEnabled")
    public boolean isCocoricoAtSunriseEnabled() {
        return getConfigValue("play.cocorico.at.sunrise.enabled", cocoricoAtSunriseEnabled,
                Boolean::valueOf);
    }

    @CacheEvict(value = "cocoricoAtSunriseEnabled")
    public void setCocoricoAtSunriseEnabled(boolean enabled) {
        setConfigValue("play.cocorico.at.sunrise.enabled", enabled, null);
    }

    @Cacheable(value = "songAtSunsetEnabled")
    public boolean isSongAtSunsetEnabled() {
        return getConfigValue("play.song.at.sunset", songAtSunsetEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = "songAtSunsetEnabled")
    public void setSongAtSunsetEnabled(boolean enabled) {
        setConfigValue("play.song.at.sunset", enabled, null);
    }

    // ============================================================================
    // Notification Toggles
    // ============================================================================

    @Cacheable(value = "emailNotificationEnabled")
    public boolean isEmailNotificationEnabled() {
        return getConfigValue("email.notification.enabled", emailNotificationEnabled,
                Boolean::valueOf);
    }

    @CacheEvict(value = "emailNotificationEnabled")
    public void setEmailNotificationEnabled(boolean enabled) {
        setConfigValue("email.notification.enabled", enabled, null);
    }

    @Cacheable(value = "weatherInfoEnabled")
    public boolean isWeatherInfoEnabled() {
        return getConfigValue("weather.info.enabled", weatherInfoEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = "weatherInfoEnabled")
    public void setWeatherInfoEnabled(boolean enabled) {
        setConfigValue("weather.info.enabled", enabled, null);
    }

    // ============================================================================
    // Sun Schedule Toggle
    // ============================================================================
    //
    // When true, the door opening time is clamped to 8:00 AM even if the real
    // sunrise is earlier — handy in summer so the chickens don't wake the
    // neighbours at 5 AM.

    @Cacheable(value = "sunriseForceAt8")
    public boolean isSunriseForceAt8() {
        return getConfigValue("suntime.sunrise.force_at_8", sunriseForceAt8, Boolean::valueOf);
    }

    @CacheEvict(value = {"sunriseForceAt8", "door-opening", "light-on"}, allEntries = true)
    public void setSunriseForceAt8(boolean force) {
        setConfigValue("suntime.sunrise.force_at_8", force, null);
    }

    // ============================================================================
    // Camera Image Quality
    // ============================================================================

    @Cacheable(value = "cameraBrightness")
    public int getCameraBrightness() {
        return getConfigValue("camera.brightness", cameraBrightness, Integer::parseInt);
    }

    @CacheEvict(value = "cameraBrightness")
    public void setCameraBrightness(int brightness) {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be 0..100, got " + brightness);
        }
        setConfigValue("camera.brightness", brightness, null);
    }

    /**
     * Camera rotation in degrees, restricted to the four right-angle values that
     * the underlying camera library actually supports.
     */
    @Cacheable(value = "cameraRotation")
    public int getCameraRotation() {
        return getConfigValue("camera.rotation", cameraRotation, Integer::parseInt);
    }

    @CacheEvict(value = "cameraRotation")
    public void setCameraRotation(int degrees) {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
            throw new IllegalArgumentException(
                    "Rotation must be one of 0/90/180/270, got " + degrees);
        }
        setConfigValue("camera.rotation", degrees, null);
    }

    // ============================================================================
    // Weather Provider Settings
    // ============================================================================

    @Cacheable(value = "weatherInfoUrl")
    public String getWeatherInfoUrl() {
        return getConfigValue("weather.info.url", weatherInfoUrl, s -> s);
    }

    /**
     * Sets the weather provider's HTTP URL. The template must keep the {@code {latitude}},
     * {@code {longitude}}, {@code {key}} placeholders for {@link org.jibe77.hermanas.client.weather.WeatherClient}
     * to interpolate them — we do not validate that here so a future migration to a
     * different provider with a different signature stays possible without code change.
     */
    @CacheEvict(value = "weatherInfoUrl")
    public void setWeatherInfoUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Weather URL must not be empty");
        }
        setConfigValue("weather.info.url", url.trim(), null);
    }

    @Cacheable(value = "weatherInfoKey")
    public String getWeatherInfoKey() {
        return getConfigValue("weather.info.key", weatherInfoKey, s -> s);
    }

    /**
     * Sets the weather API key. The empty string is rejected — it would silently make
     * every weather call fail with 401. To turn the feature off use
     * {@link #setWeatherInfoEnabled(boolean)} instead.
     */
    @CacheEvict(value = "weatherInfoKey")
    public void setWeatherInfoKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "API key must not be empty. To turn weather off, use the enabled flag.");
        }
        setConfigValue("weather.info.key", key.trim(), null);
    }

    // ============================================================================
    // Email Addresses
    // ============================================================================
    //
    // Light email validation only: the JavaMail provider will raise the real error
    // upon send anyway, and stricter regex tends to reject perfectly legal addresses.

    @Cacheable(value = "emailNotificationTo")
    public String getEmailNotificationTo() {
        return getConfigValue("email.notification.to", emailNotificationTo, s -> s);
    }

    @CacheEvict(value = "emailNotificationTo")
    public void setEmailNotificationTo(String to) {
        if (to == null || !to.contains("@")) {
            throw new IllegalArgumentException("Invalid 'to' email: " + to);
        }
        setConfigValue("email.notification.to", to.trim(), null);
    }

    @Cacheable(value = "emailNotificationFrom")
    public String getEmailNotificationFrom() {
        return getConfigValue("email.notification.from", emailNotificationFrom, s -> s);
    }

    @CacheEvict(value = "emailNotificationFrom")
    public void setEmailNotificationFrom(String from) {
        if (from == null || !from.contains("@")) {
            throw new IllegalArgumentException("Invalid 'from' email: " + from);
        }
        setConfigValue("email.notification.from", from.trim(), null);
    }
}
