package org.jibe77.hermanas.service.config;

import org.apache.commons.lang3.StringUtils;
import org.jibe77.hermanas.data.entity.EventType;
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

import java.util.List;
import java.util.Locale;
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
    // Email recipients are per-user (HermanasUser.notificationsEnabled). There is no
    // global "email enabled" flag — opting everyone out individually is the kill switch.

    @Value("${weather.info.enabled}")
    private boolean weatherInfoEnabled;

    // ─── Sun schedule ────────────────────────────────────────────────────────────

    @Value("${door.opening.force.enabled}")
    private boolean doorOpeningForceEnabled;

    @Value("${door.opening.force.time}")
    private String doorOpeningForceTime;

    @Value("${door.closing.force.enabled}")
    private boolean doorClosingForceEnabled;

    @Value("${door.closing.force.time}")
    private String doorClosingForceTime;

    @Value("${suntime.latitude}")
    private double latitude;

    @Value("${suntime.longitude}")
    private double longitude;

    // ─── Camera image quality ─────────────────────────────────────────────────────

    @Value("${camera.brightness}")
    private int cameraBrightness;

    @Value("${camera.rotation}")
    private int cameraRotation;

    @Value("${camera.awb:}")
    private String cameraAwb;

    @Value("${camera.awbgains:}")
    private String cameraAwbGains;

    @Value("${camera.roi:}")
    private String cameraRoi;

    @Value("${camera.mode:}")
    private String cameraMode;

    @Value("${camera.shutter:}")
    private String cameraShutter;

    @Value("${camera.gain:}")
    private String cameraGain;

    @Value("${camera.regular.width:1096}")
    private int cameraRegularWidth;

    @Value("${camera.regular.height:822}")
    private int cameraRegularHeight;

    @Value("${camera.regular.delay:500}")
    private int cameraRegularDelay;

    @Value("${camera.high.width:1640}")
    private int cameraHighWidth;

    @Value("${camera.high.height:1232}")
    private int cameraHighHeight;

    @Value("${camera.high.delay:1000}")
    private int cameraHighDelay;

    @Value("${camera.regular.quality}")
    private int cameraRegularQuality;

    @Value("${camera.high.quality}")
    private int cameraHighQuality;

    // ─── Weather provider settings ────────────────────────────────────────────────

    @Value("${weather.info.url}")
    private String weatherInfoUrl;

    @Value("${weather.info.key}")
    private String weatherInfoKey;

    // ─── AI inference endpoint ────────────────────────────────────────────────────

    @Value("${ai.inference.url:}")
    private String aiInferenceUrl;

    @Value("${ai.inference.model:focus}")
    private String aiInferenceModel;

    @Value("${ai.inference.cache.ttl-ms:120000}")
    private long aiInferenceCacheTtlMs;

    @Value("${ai.inference.prompt:}")
    private String aiInferencePrompt;

    @Value("${ai.inference.connect-timeout-ms:15000}")
    private int aiInferenceConnectTimeoutMs;

    @Value("${ai.inference.read-timeout-ms:180000}")
    private int aiInferenceReadTimeoutMs;

    @Value("${ai.inference.retry.max-attempts:3}")
    private int aiInferenceRetryMaxAttempts;

    @Value("${ai.inference.retry.initial-backoff-ms:2000}")
    private long aiInferenceRetryInitialBackoffMs;

    @Value("${ai.inference.retry.max-backoff-ms:10000}")
    private long aiInferenceRetryMaxBackoffMs;

    // ─── Email sender ─────────────────────────────────────────────────────────────

    // Default left empty intentionally — production deployments may not have
    // this property in their application.properties, and we'd rather boot with
    // a null From (caught loudly in getEmailNotificationFrom) than fail at
    // startup with a BeanCreationException.
    @Value("${email.notification.from:}")
    private String emailNotificationFrom;

    // ─── SMTP transport settings ──────────────────────────────────────────────────
    // Mirrors spring.mail.* in application.properties. The values stored here win
    // over the @Value defaults at send time (see EmailService.resolveSender).
    // Empty/blank => keep the application.properties fallback.

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:25}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean mailSmtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean mailStartTlsEnable;

    ParameterRepository parameterRepository;

    @Autowired(required = false)
    HermanasMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    /**
     * Dedicated logger for configuration audit trail. Routed to
     * {@code audit_config.txt} by {@code logback-spring.xml} so operators
     * can review who changed which knob without grepping the main log.
     */
    private static final Logger configAuditLogger = LoggerFactory.getLogger("CONFIG_AUDIT");

    // Optional injection — small Spring contexts in tests (e.g. ConsumptionModeControllerTest)
    // do not load EventService, and we degrade gracefully to "no journal entry" in that case.
    @Autowired(required = false)
    private org.jibe77.hermanas.service.event.EventService eventService;

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

        // The parameter.entry_key column has a UNIQUE constraint, so a plain
        // `save(new Parameter(key, value))` blows up with a
        // DataIntegrityViolationException on the second write of the same key.
        // Upsert by looking the row up first, mutating its value if it exists,
        // creating it otherwise. findByEntryKey is the only method on the
        // repository besides the standard CrudRepository ones.
        Parameter parameter = parameterRepository.findByEntryKey(key);
        String oldValue = parameter != null ? parameter.getEntryValue() : null;
        if (parameter == null) {
            parameter = new Parameter();
            parameter.setEntryKey(key);
        }
        parameter.setEntryValue(String.valueOf(value));

        logger.info("Saving config to database: {} = {}", key, value);
        parameterRepository.save(parameter);

        auditConfigChange(key, oldValue, value);

        // Record configuration change metric (if metrics available)
        if (metrics != null) {
            metrics.recordConfigChange(key);
        }
    }

    /**
     * Emits one structured line per persisted config change to the
     * {@code CONFIG_AUDIT} logger (routed to {@code audit_config.txt}) and a
     * matching {@code CONFIG_CHANGED} business event so the change also lands
     * on the Journalisation page next to door/light/fan rows.
     *
     * <p>Audit log format (pipe-separated for grep-friendliness):</p>
     * <pre>user=&lt;login|anonymous&gt; | key=&lt;key&gt; | old=&lt;value&gt; | new=&lt;value&gt;</pre>
     */
    private void auditConfigChange(String key, Object oldValue, Object newValue) {
        String user = org.jibe77.hermanas.service.event.EventService.currentUsername();
        String oldRendered = oldValue == null ? "<unset>" : String.valueOf(oldValue);
        String newRendered = newValue == null ? "<unset>" : String.valueOf(newValue);
        configAuditLogger.info("user={} | key={} | old={} | new={}",
                user == null ? "anonymous" : user, key, oldRendered, newRendered);
        if (eventService != null) {
            // Mask secret-looking keys before they reach the journal so the
            // Journalisation page never surfaces a fresh API key or mail password
            // in plain text. The CONFIG_AUDIT file still gets the redacted form too.
            String redactedNew = isSecretKey(key) ? "<redacted>" : newRendered;
            String redactedOld = isSecretKey(key) ? "<redacted>" : oldRendered;
            eventService.record(EventType.CONFIG_CHANGED,
                    "key=" + key + " old=" + redactedOld + " new=" + redactedNew);
        }
    }

    /**
     * Returns true if the given key holds a secret that must never appear in
     * plain text in the journal (mail password, API keys, etc.). Conservative —
     * any key containing "password", "key", "secret" or "token" is treated as
     * sensitive. The latitude/longitude pair is also masked because the coop
     * coordinates are private.
     */
    private static boolean isSecretKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase();
        return k.contains("password") || k.contains("secret") || k.contains("token")
                || k.endsWith(".key") || k.contains(".key.")
                || k.equals("suntime.latitude") || k.equals("suntime.longitude");
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
    @CacheEvict(value = "lightSecurityTimerDelayEco", allEntries = true)
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
    @CacheEvict(value = "lightSecurityTimerDelayRegular", allEntries = true)
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
    @CacheEvict(value = "lightSecurityTimerDelaySunny", allEntries = true)
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
    @CacheEvict(value = "fanSecurityTimerDelayEco", allEntries = true)
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
    @CacheEvict(value = "fanSecurityTimerDelayRegular", allEntries = true)
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
    @CacheEvict(value = "fanSecurityTimerDelaySunny", allEntries = true)
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
    @CacheEvict(value = "musicSecurityTimerDelayEco", allEntries = true)
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
    @CacheEvict(value = "musicSecurityTimerDelayRegular", allEntries = true)
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
    @CacheEvict(value = "musicSecurityTimerDelaySunny", allEntries = true)
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
    @CacheEvict(value = "consumptionModeEcoForce", allEntries = true)
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
    @CacheEvict(value = "wifiDisabledInEcoMode", allEntries = true)
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
    @CacheEvict(value = "wifiDisabledInSunnyMode", allEntries = true)
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
    @CacheEvict(value = "wifiDisabledInRegularMode", allEntries = true)
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
    @CacheEvict(value = "selectedPlaylist", allEntries = true)
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
    @CacheEvict(value = "musicVolumeRegular", allEntries = true)
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

    @CacheEvict(value = "doorOpeningPosition", allEntries = true)
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

    @CacheEvict(value = "doorClosingPosition", allEntries = true)
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

    @CacheEvict(value = "doorOpeningDuration", allEntries = true)
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

    @CacheEvict(value = "doorClosingDuration", allEntries = true)
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

    @CacheEvict(value = "cocoricoAtSunriseEnabled", allEntries = true)
    public void setCocoricoAtSunriseEnabled(boolean enabled) {
        setConfigValue("play.cocorico.at.sunrise.enabled", enabled, null);
    }

    @Cacheable(value = "songAtSunsetEnabled")
    public boolean isSongAtSunsetEnabled() {
        return getConfigValue("play.song.at.sunset", songAtSunsetEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = "songAtSunsetEnabled", allEntries = true)
    public void setSongAtSunsetEnabled(boolean enabled) {
        setConfigValue("play.song.at.sunset", enabled, null);
    }

    // ============================================================================
    // Notification Toggles
    // ============================================================================

    @Cacheable(value = "weatherInfoEnabled")
    public boolean isWeatherInfoEnabled() {
        return getConfigValue("weather.info.enabled", weatherInfoEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = "weatherInfoEnabled", allEntries = true)
    public void setWeatherInfoEnabled(boolean enabled) {
        setConfigValue("weather.info.enabled", enabled, null);
    }

    // ============================================================================
    // Door force-schedule overrides
    // ============================================================================
    //
    // When enabled=true, the corresponding open/close time is used verbatim
    // (HH:mm) and fully replaces the sunrise/sunset computation. Every derived
    // schedule (door open, door close, light on) shifts accordingly since they
    // all consume the values returned by SunTimeUtils.
    // Leave enabled=false to fall back to the sun-based computation.

    @Cacheable(value = "doorOpeningForceEnabled")
    public boolean isDoorOpeningForceEnabled() {
        return getConfigValue("door.opening.force.enabled", doorOpeningForceEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = {"doorOpeningForceEnabled", "door-opening", "light-on"}, allEntries = true)
    public void setDoorOpeningForceEnabled(boolean enabled) {
        setConfigValue("door.opening.force.enabled", enabled, null);
    }

    @Cacheable(value = "doorOpeningForceTime")
    public String getDoorOpeningForceTime() {
        return getConfigValue("door.opening.force.time", doorOpeningForceTime, s -> s);
    }

    @CacheEvict(value = {"doorOpeningForceTime", "door-opening", "light-on"}, allEntries = true)
    public void setDoorOpeningForceTime(String time) {
        parseHhmmOrThrow("door.opening.force.time", time);
        setConfigValue("door.opening.force.time", time, null);
    }

    @Cacheable(value = "doorClosingForceEnabled")
    public boolean isDoorClosingForceEnabled() {
        return getConfigValue("door.closing.force.enabled", doorClosingForceEnabled, Boolean::valueOf);
    }

    @CacheEvict(value = {"doorClosingForceEnabled", "door-closing", "light-on"}, allEntries = true)
    public void setDoorClosingForceEnabled(boolean enabled) {
        setConfigValue("door.closing.force.enabled", enabled, null);
    }

    @Cacheable(value = "doorClosingForceTime")
    public String getDoorClosingForceTime() {
        return getConfigValue("door.closing.force.time", doorClosingForceTime, s -> s);
    }

    @CacheEvict(value = {"doorClosingForceTime", "door-closing", "light-on"}, allEntries = true)
    public void setDoorClosingForceTime(String time) {
        parseHhmmOrThrow("door.closing.force.time", time);
        setConfigValue("door.closing.force.time", time, null);
    }

    private static void parseHhmmOrThrow(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid value for '" + key + "': null or empty");
        }
        try {
            java.time.LocalTime.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + key + "': expected HH:mm, got '" + value + "'");
        }
    }

    // ============================================================================
    // Camera Image Quality
    // ============================================================================

    @Cacheable(value = "cameraBrightness")
    public int getCameraBrightness() {
        return getConfigValue("camera.brightness", cameraBrightness, Integer::parseInt);
    }

    @CacheEvict(value = "cameraBrightness", allEntries = true)
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

    @CacheEvict(value = "cameraRotation", allEntries = true)
    public void setCameraRotation(int degrees) {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
            throw new IllegalArgumentException(
                    "Rotation must be one of 0/90/180/270, got " + degrees);
        }
        setConfigValue("camera.rotation", degrees, null);
    }

    /**
     * Modes de balance des blancs acceptés par {@code rpicam-still}.
     *
     * <p>Un mode <em>compense</em> la lumière qu'il suppose : plus la température
     * supposée est basse, plus l'image est refroidie. Du plus froid au plus chaud —
     * {@code incandescent} (~2500 K), {@code tungsten} (~3000 K), {@code indoor},
     * {@code fluorescent} (~4000 K), {@code daylight} (~5500 K), {@code cloudy}.</p>
     */
    public static final List<String> CAMERA_AWB_MODES = List.of(
            "auto", "incandescent", "tungsten", "fluorescent",
            "indoor", "daylight", "cloudy");

    /**
     * Balance des blancs de la caméra.
     *
     * <p>La photo est toujours prise avec la lampe du poulailler allumée : l'éclairage
     * est constant, donc un mode fixe converge instantanément là où l'automatique a
     * besoin de plusieurs images — ce que le {@code --timeout} de 500 ms ne lui laisse
     * pas. En {@code auto}, la capture partait sur des gains provisoires trop rouges,
     * d'où les reflets rosés constatés.</p>
     */
    @Cacheable(value = "cameraAwb")
    public String getCameraAwb() {
        return getConfigValue("camera.awb", cameraAwb, v -> v);
    }

    @CacheEvict(value = "cameraAwb", allEntries = true)
    public void setCameraAwb(String mode) {
        String value = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        // Le vide est accepté : il rend la main à l'automatique.
        if (!value.isEmpty() && !CAMERA_AWB_MODES.contains(value)) {
            throw new IllegalArgumentException(
                    "AWB mode must be one of " + CAMERA_AWB_MODES + " (or empty), got " + mode);
        }
        setConfigValue("camera.awb", value, null);
    }

    /**
     * Gains rouge et bleu imposés directement, au format {@code "R,B"}.
     *
     * <p>Prioritaire sur {@link #getCameraAwb()} : {@code rpicam-still} ignore
     * {@code --awb} dès que {@code --awbgains} est fourni. Utile quand aucun mode
     * prédéfini ne corrige la dominante — pour atténuer le rouge, baisser R et
     * monter B (1.4,1.8 puis 1.2,2.0 puis 1.0,2.2).</p>
     *
     * <p>Vide = on s'en remet au mode AWB.</p>
     */
    @Cacheable(value = "cameraAwbGains")
    public String getCameraAwbGains() {
        return getConfigValue("camera.awbgains", cameraAwbGains, v -> v);
    }

    @CacheEvict(value = "cameraAwbGains", allEntries = true)
    public void setCameraAwbGains(String gains) {
        String value = gains == null ? "" : gains.trim();
        if (!value.isEmpty() && !value.matches("\\d+(\\.\\d+)?,\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException(
                    "AWB gains must be \"R,B\" with positive decimals (e.g. 1.2,2.0), got " + gains);
        }
        setConfigValue("camera.awbgains", value, null);
    }

    /**
     * Mode capteur imposé ({@code --mode}), {@code "largeur:hauteur"}. Vide = choix
     * automatique de libcamera.
     *
     * <p><b>Pourquoi ce réglage existe.</b> Sous ~790 px de hauteur de sortie,
     * libcamera bascule sur le mode 640×480, qui est <b>recadré au centre</b> :
     * l'image sort zoomée et pixelisée. Forcer un mode plein champ
     * ({@code 1640:1232} ou {@code 3280:2464}) libère la hauteur de sortie, ce
     * qui est indispensable dès qu'on combine un ROI avec une hauteur réduite.</p>
     */
    @Cacheable(value = "cameraMode")
    public String getCameraMode() {
        return getConfigValue("camera.mode", cameraMode, v -> v);
    }

    @CacheEvict(value = "cameraMode", allEntries = true)
    public void setCameraMode(String mode) {
        String value = mode == null ? "" : mode.trim();
        if (!value.isEmpty() && !value.matches("\\d+:\\d+(:\\d+)?(:[PU])?")) {
            throw new IllegalArgumentException(
                    "Mode must be \"width:height\", optionally \":bit-depth\" and \":P\"/\":U\" "
                            + "(e.g. 1640:1232), got " + mode);
        }
        setConfigValue("camera.mode", value, null);
    }

    /**
     * Temps de pose en microsecondes ({@code --shutter}). Vide = automatique.
     *
     * <p>Fixer l'exposition supprime le temps de convergence de l'AEC, ce qui
     * autorise un {@code delay} plus court. Mais contrairement à la balance des
     * blancs, la bonne valeur dépend de la lumière ambiante — elle change entre
     * midi et le crépuscule, même lampe allumée.</p>
     *
     * <p>Trop court : image sombre. Trop long : flou de bougé sur les poules.</p>
     */
    @Cacheable(value = "cameraShutter")
    public String getCameraShutter() {
        return getConfigValue("camera.shutter", cameraShutter, v -> v);
    }

    @CacheEvict(value = "cameraShutter", allEntries = true)
    public void setCameraShutter(String shutter) {
        String value = shutter == null ? "" : shutter.trim();
        if (!value.isEmpty()) {
            long microseconds;
            try {
                microseconds = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Shutter must be an integer number of microseconds, got " + shutter);
            }
            // Borne haute à 10 s : au-delà on est en pose longue, hors usage ici,
            // et le processus dépasserait le timeout de capture.
            if (microseconds < 1 || microseconds > 10_000_000L) {
                throw new IllegalArgumentException(
                        "Shutter must be between 1 and 10000000 microseconds (10 s), got " + shutter);
            }
        }
        setConfigValue("camera.shutter", value, null);
    }

    /**
     * Gain analogique ({@code --gain}). Vide = automatique, {@code 1} = aucun gain.
     *
     * <p>Monter éclaircit l'image mais ajoute du bruit ; au-delà de 8 le grain
     * devient visible.</p>
     */
    @Cacheable(value = "cameraGain")
    public String getCameraGain() {
        return getConfigValue("camera.gain", cameraGain, v -> v);
    }

    @CacheEvict(value = "cameraGain", allEntries = true)
    public void setCameraGain(String gain) {
        String value = gain == null ? "" : gain.trim();
        if (!value.isEmpty()) {
            double parsed;
            try {
                parsed = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Gain must be a decimal number, got " + gain);
            }
            if (parsed < 1.0 || parsed > 16.0) {
                throw new IllegalArgumentException("Gain must be between 1.0 and 16.0, got " + gain);
            }
        }
        setConfigValue("camera.gain", value, null);
    }

    /**
     * Zone du capteur réellement lue, {@code "x,y,largeur,hauteur"} en valeurs
     * normalisées 0-1. Vide = capteur entier.
     *
     * <p>⚠️ {@code rpicam-still} recadre <em>puis rééchantillonne</em> à la taille de
     * sortie demandée : c'est un zoom numérique, pas un simple rognage. Pour retirer
     * une zone sans agrandir le reste, la hauteur de sortie doit être ajustée dans la
     * même proportion — couper 20 % en haut ({@code 0,0.2,1,0.8}) suppose de passer
     * la hauteur de 822 à 658. Sinon l'image est étirée verticalement.</p>
     */
    @Cacheable(value = "cameraRoi")
    public String getCameraRoi() {
        return getConfigValue("camera.roi", cameraRoi, v -> v);
    }

    @CacheEvict(value = "cameraRoi", allEntries = true)
    public void setCameraRoi(String roi) {
        String value = roi == null ? "" : roi.trim();
        if (!value.isEmpty()) {
            String[] parts = value.split(",");
            if (parts.length != 4) {
                throw new IllegalArgumentException(
                        "ROI must be \"x,y,width,height\" with four values, got " + roi);
            }
            double[] v = new double[4];
            for (int i = 0; i < 4; i++) {
                try {
                    v[i] = Double.parseDouble(parts[i].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "ROI values must be decimals between 0 and 1, got " + roi, e);
                }
                if (v[i] < 0 || v[i] > 1) {
                    throw new IllegalArgumentException(
                            "ROI values must be between 0 and 1, got " + parts[i].trim());
                }
            }
            // Une zone qui déborde du capteur serait rejetée par rpicam-still, mais
            // avec un message obscur : autant l'expliquer ici.
            if (v[2] <= 0 || v[3] <= 0) {
                throw new IllegalArgumentException("ROI width and height must be greater than 0");
            }
            if (v[0] + v[2] > 1.0001 || v[1] + v[3] > 1.0001) {
                throw new IllegalArgumentException(
                        "ROI extends past the sensor: x+width and y+height must not exceed 1");
            }
        }
        setConfigValue("camera.roi", value, null);
    }

    // ─── Dimensions et délais de capture ──────────────────────────────────────────
    //
    // Bornes volontairement larges : 64 px au minimum pour écarter une saisie
    // absurde, 4096 au maximum — au-delà de la définition native du capteur
    // (3280x2464), rpicam-still suréchantillonnerait sans gain de détail.
    //
    // ⚠️ Conserver un ratio 4:3. En 16:9, libcamera sélectionne un mode capteur
    // RECADRÉ et le champ du fisheye est amputé.

    private static final int MIN_DIMENSION = 64;
    private static final int MAX_DIMENSION = 4096;
    private static final int MAX_DELAY_MS = 30000;

    private static int checkDimension(String label, int value) {
        if (value < MIN_DIMENSION || value > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    label + " must be between " + MIN_DIMENSION + " and " + MAX_DIMENSION
                            + ", got " + value);
        }
        return value;
    }

    private static int checkDelay(String label, int value) {
        if (value < 0 || value > MAX_DELAY_MS) {
            throw new IllegalArgumentException(
                    label + " must be between 0 and " + MAX_DELAY_MS + " ms, got " + value);
        }
        return value;
    }

    @Cacheable(value = "cameraRegularWidth")
    public int getCameraRegularWidth() {
        return getConfigValue("camera.regular.width", cameraRegularWidth, Integer::parseInt);
    }

    @CacheEvict(value = "cameraRegularWidth", allEntries = true)
    public void setCameraRegularWidth(int width) {
        setConfigValue("camera.regular.width", checkDimension("Width", width), null);
    }

    @Cacheable(value = "cameraRegularHeight")
    public int getCameraRegularHeight() {
        return getConfigValue("camera.regular.height", cameraRegularHeight, Integer::parseInt);
    }

    @CacheEvict(value = "cameraRegularHeight", allEntries = true)
    public void setCameraRegularHeight(int height) {
        setConfigValue("camera.regular.height", checkDimension("Height", height), null);
    }

    /**
     * Temps laissé à l'auto-exposition pour converger avant le déclenchement
     * ({@code --timeout}). Trop court, la capture part sur des gains provisoires.
     */
    @Cacheable(value = "cameraRegularDelay")
    public int getCameraRegularDelay() {
        return getConfigValue("camera.regular.delay", cameraRegularDelay, Integer::parseInt);
    }

    @CacheEvict(value = "cameraRegularDelay", allEntries = true)
    public void setCameraRegularDelay(int delayMs) {
        setConfigValue("camera.regular.delay", checkDelay("Delay", delayMs), null);
    }

    @Cacheable(value = "cameraHighWidth")
    public int getCameraHighWidth() {
        return getConfigValue("camera.high.width", cameraHighWidth, Integer::parseInt);
    }

    @CacheEvict(value = "cameraHighWidth", allEntries = true)
    public void setCameraHighWidth(int width) {
        setConfigValue("camera.high.width", checkDimension("Width", width), null);
    }

    @Cacheable(value = "cameraHighHeight")
    public int getCameraHighHeight() {
        return getConfigValue("camera.high.height", cameraHighHeight, Integer::parseInt);
    }

    @CacheEvict(value = "cameraHighHeight", allEntries = true)
    public void setCameraHighHeight(int height) {
        setConfigValue("camera.high.height", checkDimension("Height", height), null);
    }

    @Cacheable(value = "cameraHighDelay")
    public int getCameraHighDelay() {
        return getConfigValue("camera.high.delay", cameraHighDelay, Integer::parseInt);
    }

    @CacheEvict(value = "cameraHighDelay", allEntries = true)
    public void setCameraHighDelay(int delayMs) {
        setConfigValue("camera.high.delay", checkDelay("Delay", delayMs), null);
    }

    @Cacheable(value = "cameraRegularQuality")
    public int getCameraRegularQuality() {
        return getConfigValue("camera.regular.quality", cameraRegularQuality, Integer::parseInt);
    }

    @CacheEvict(value = "cameraRegularQuality", allEntries = true)
    public void setCameraRegularQuality(int quality) {
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Quality must be 1..100, got " + quality);
        }
        setConfigValue("camera.regular.quality", quality, null);
    }

    @Cacheable(value = "cameraHighQuality")
    public int getCameraHighQuality() {
        return getConfigValue("camera.high.quality", cameraHighQuality, Integer::parseInt);
    }

    @CacheEvict(value = "cameraHighQuality", allEntries = true)
    public void setCameraHighQuality(int quality) {
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Quality must be 1..100, got " + quality);
        }
        setConfigValue("camera.high.quality", quality, null);
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
    @CacheEvict(value = "weatherInfoUrl", allEntries = true)
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
    @CacheEvict(value = "weatherInfoKey", allEntries = true)
    public void setWeatherInfoKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "API key must not be empty. To turn weather off, use the enabled flag.");
        }
        setConfigValue("weather.info.key", key.trim(), null);
    }

    // ============================================================================
    // AI inference endpoint
    // ============================================================================
    // URL of the local LLM service used by the WIP /camera/analyze endpoint. The
    // value is intentionally lax — anything from "http://localhost:11434/api/chat"
    // to a custom REST adapter is valid. Empty string means "not configured yet".

    @Cacheable(value = "aiInferenceUrl")
    public String getAiInferenceUrl() {
        return getConfigValue("ai.inference.url", aiInferenceUrl, s -> s);
    }

    @CacheEvict(value = "aiInferenceUrl", allEntries = true)
    public void setAiInferenceUrl(String url) {
        // Allow clearing the URL by passing an empty string — that effectively
        // marks the AI integration as "not configured", which is a legitimate state.
        setConfigValue("ai.inference.url", url == null ? "" : url.trim(), null);
    }

    /**
     * Name of the multimodal model exposed by the inference server. Matches the
     * {@code "model"} field of an OpenAI-compatible /v1/chat/completions request.
     * Defaults to {@code "focus"} (the qwen2.5-vl mapping used by our llama.cpp).
     */
    @Cacheable(value = "aiInferenceModel")
    public String getAiInferenceModel() {
        String configured = getConfigValue("ai.inference.model", aiInferenceModel, s -> s);
        return configured == null || configured.trim().isEmpty() ? "focus" : configured.trim();
    }

    @CacheEvict(value = "aiInferenceModel", allEntries = true)
    public void setAiInferenceModel(String model) {
        setConfigValue("ai.inference.model", model == null ? "" : model.trim(), null);
    }

    /**
     * Cache TTL (in milliseconds) for {@link org.jibe77.hermanas.client.ai.AiVisionCache}.
     * A non-positive value disables caching. Defaults to 120 000 ms (2 minutes).
     */
    @Cacheable(value = "aiInferenceCacheTtlMs")
    public long getAiInferenceCacheTtlMs() {
        return getConfigValue("ai.inference.cache.ttl-ms", aiInferenceCacheTtlMs, Long::parseLong);
    }

    @CacheEvict(value = "aiInferenceCacheTtlMs", allEntries = true)
    public void setAiInferenceCacheTtlMs(long ttlMs) {
        setConfigValue("ai.inference.cache.ttl-ms", ttlMs, null);
    }

    /**
     * Custom prompt sent to the multimodal model. English-only — the SPA tells
     * the model in which language to answer via an instruction appended at
     * runtime. Empty string means "use the built-in chicken-coop prompt"
     * (see {@code CameraPromptBuilder#BASE_PROMPT}).
     */
    @Cacheable(value = "aiInferencePrompt")
    public String getAiInferencePrompt() {
        return getConfigValue("ai.inference.prompt", aiInferencePrompt, s -> s);
    }

    @CacheEvict(value = "aiInferencePrompt", allEntries = true)
    public void setAiInferencePrompt(String prompt) {
        setConfigValue("ai.inference.prompt", prompt == null ? "" : prompt.trim(), null);
    }

    /**
     * HTTP connect timeout (ms) for the call to the inference server. Tuned
     * up from the original 5 s so a momentarily swapped-out llama.cpp still
     * answers the TCP handshake. Changes take effect on the next reboot
     * because {@link org.jibe77.hermanas.client.ai.AiVisionClient} builds
     * its RestTemplate once at construction.
     */
    @Cacheable(value = "aiInferenceConnectTimeoutMs")
    public int getAiInferenceConnectTimeoutMs() {
        return getConfigValue("ai.inference.connect-timeout-ms",
                aiInferenceConnectTimeoutMs, Integer::parseInt);
    }

    @CacheEvict(value = "aiInferenceConnectTimeoutMs", allEntries = true)
    public void setAiInferenceConnectTimeoutMs(int ms) {
        setConfigValue("ai.inference.connect-timeout-ms", ms, null);
    }

    /**
     * HTTP read timeout (ms) for the call to the inference server. Covers
     * the actual inference time — vision on a Pi-class CPU is slow, so
     * keep this generous (default 180 s).
     */
    @Cacheable(value = "aiInferenceReadTimeoutMs")
    public int getAiInferenceReadTimeoutMs() {
        return getConfigValue("ai.inference.read-timeout-ms",
                aiInferenceReadTimeoutMs, Integer::parseInt);
    }

    @CacheEvict(value = "aiInferenceReadTimeoutMs", allEntries = true)
    public void setAiInferenceReadTimeoutMs(int ms) {
        setConfigValue("ai.inference.read-timeout-ms", ms, null);
    }

    /**
     * Total number of attempts (initial + retries) for the inference HTTP
     * call. Only connect-phase failures are retried — see AiVisionClient.
     */
    @Cacheable(value = "aiInferenceRetryMaxAttempts")
    public int getAiInferenceRetryMaxAttempts() {
        return getConfigValue("ai.inference.retry.max-attempts",
                aiInferenceRetryMaxAttempts, Integer::parseInt);
    }

    @CacheEvict(value = "aiInferenceRetryMaxAttempts", allEntries = true)
    public void setAiInferenceRetryMaxAttempts(int attempts) {
        setConfigValue("ai.inference.retry.max-attempts", attempts, null);
    }

    @Cacheable(value = "aiInferenceRetryInitialBackoffMs")
    public long getAiInferenceRetryInitialBackoffMs() {
        return getConfigValue("ai.inference.retry.initial-backoff-ms",
                aiInferenceRetryInitialBackoffMs, Long::parseLong);
    }

    @CacheEvict(value = "aiInferenceRetryInitialBackoffMs", allEntries = true)
    public void setAiInferenceRetryInitialBackoffMs(long ms) {
        setConfigValue("ai.inference.retry.initial-backoff-ms", ms, null);
    }

    @Cacheable(value = "aiInferenceRetryMaxBackoffMs")
    public long getAiInferenceRetryMaxBackoffMs() {
        return getConfigValue("ai.inference.retry.max-backoff-ms",
                aiInferenceRetryMaxBackoffMs, Long::parseLong);
    }

    @CacheEvict(value = "aiInferenceRetryMaxBackoffMs", allEntries = true)
    public void setAiInferenceRetryMaxBackoffMs(long ms) {
        setConfigValue("ai.inference.retry.max-backoff-ms", ms, null);
    }

    // ============================================================================
    // GPS coordinates (used by both SunTimeUtils and WeatherClient)
    // ============================================================================
    //
    // These coordinates feed two independent subsystems — sun-time calculations
    // (door opening / closing schedule) and the weather lookup. A bad value here
    // silently shifts the entire daily schedule, so reject values outside the
    // standard WGS84 ranges.

    @Cacheable(value = "latitude")
    public double getLatitude() {
        return getConfigValue("suntime.latitude", latitude, Double::parseDouble);
    }

    @CacheEvict(value = "latitude", allEntries = true)
    public void setLatitude(double value) {
        if (value < -90.0 || value > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90: " + value);
        }
        setConfigValue("suntime.latitude", value, null);
    }

    @Cacheable(value = "longitude")
    public double getLongitude() {
        return getConfigValue("suntime.longitude", longitude, Double::parseDouble);
    }

    @CacheEvict(value = "longitude", allEntries = true)
    public void setLongitude(double value) {
        if (value < -180.0 || value > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180: " + value);
        }
        setConfigValue("suntime.longitude", value, null);
    }

    // ============================================================================
    // Email "From" Address
    // ============================================================================
    //
    // Recipients are no longer configured here — automated notifications go to every
    // user with notificationsEnabled=true (see EmailService.resolveRecipients) and
    // diagnostic test emails go to the authenticated user's own address.
    // Light email validation only: the JavaMail provider will raise the real error
    // upon send anyway, and stricter regex tends to reject perfectly legal addresses.

    // No @Cacheable here. The value is read at most a few times a day (once per
    // door open/close notification and diagnostic sends), so the DB hit is negligible,
    // and we saw in production that any cache indirection can leave a stale null in
    // place until a manual /config/refresh — dropping mails silently for hours.
    // Read straight from the source every time.
    public String getEmailNotificationFrom() {
        logger.info("returning email, default value : {}", emailNotificationFrom);
        String resolved = getConfigValue("email.notification.from", emailNotificationFrom, s -> s);
        logger.info("returning email, resolved value : {}", resolved);
        // Defensive trim: a value persisted with stray whitespace would pass
        // the !isEmpty() check inside getConfigValue but still fail the
        // !trim().isEmpty() check downstream in EmailService, producing the
        // confusing "No 'From' address configured" warning despite a value
        // visible in the admin form. Normalize here so callers always see
        // either a usable address or null.
        if (resolved == null) {
            logger.warn("email.notification.from resolved to null — neither DB nor @Value supplied a value.");
            return null;
        }
        String trimmed = resolved.trim();
        if (trimmed.isEmpty()) {
            logger.warn("email.notification.from resolved to empty/whitespace — treating as unconfigured.");
            return null;
        }
        return trimmed;
    }

    @CacheEvict(value = "emailNotificationFrom", allEntries = true)
    public void setEmailNotificationFrom(String from) {
        if (from == null || !from.contains("@")) {
            throw new IllegalArgumentException("Invalid 'from' email: " + from);
        }
        setConfigValue("email.notification.from", from.trim(), null);
    }

    // ============================================================================
    // SMTP transport (host / port / auth / TLS / credentials)
    // ============================================================================
    //
    // EmailService reads these every send and builds a JavaMailSenderImpl on the fly,
    // so a change takes effect on the *next* email — no restart needed.
    // The password is never returned in GET /api/v1/config (only a "set" flag is).

    @Cacheable(value = "mailHost")
    public String getMailHost() {
        return getConfigValue("spring.mail.host", mailHost, s -> s);
    }

    @CacheEvict(value = "mailHost", allEntries = true)
    public void setMailHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("SMTP host must not be empty");
        }
        setConfigValue("spring.mail.host", host.trim(), null);
    }

    @Cacheable(value = "mailPort")
    public int getMailPort() {
        return getConfigValue("spring.mail.port", mailPort, Integer::parseInt);
    }

    @CacheEvict(value = "mailPort", allEntries = true)
    public void setMailPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("SMTP port must be 1..65535: " + port);
        }
        setConfigValue("spring.mail.port", port, null);
    }

    @Cacheable(value = "mailUsername")
    public String getMailUsername() {
        return getConfigValue("spring.mail.username", mailUsername, s -> s);
    }

    @CacheEvict(value = "mailUsername", allEntries = true)
    public void setMailUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("SMTP username must not be null");
        }
        setConfigValue("spring.mail.username", username.trim(), null);
    }

    /**
     * Returns the stored SMTP password. Never surface this in an API response — use
     * {@link #isMailPasswordSet()} for UI display instead.
     */
    @Cacheable(value = "mailPassword")
    public String getMailPassword() {
        return getConfigValue("spring.mail.password", mailPassword, s -> s);
    }

    /**
     * Empty string is rejected: silently writing an empty password would break sends
     * with an opaque auth error. To disable auth, set the auth flag instead.
     */
    @CacheEvict(value = "mailPassword", allEntries = true)
    public void setMailPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "SMTP password must not be empty. To disable auth, turn off the auth flag.");
        }
        setConfigValue("spring.mail.password", password, null);
    }

    public boolean isMailPasswordSet() {
        String pwd = getMailPassword();
        return pwd != null && !pwd.isEmpty()
                && !"to-override-in-application-properties-file".equals(pwd);
    }

    @Cacheable(value = "mailSmtpAuth")
    public boolean isMailSmtpAuth() {
        return getConfigValue("spring.mail.properties.mail.smtp.auth", mailSmtpAuth, Boolean::parseBoolean);
    }

    @CacheEvict(value = "mailSmtpAuth", allEntries = true)
    public void setMailSmtpAuth(boolean value) {
        setConfigValue("spring.mail.properties.mail.smtp.auth", value, null);
    }

    @Cacheable(value = "mailStartTlsEnable")
    public boolean isMailStartTlsEnable() {
        return getConfigValue("spring.mail.properties.mail.smtp.starttls.enable", mailStartTlsEnable, Boolean::parseBoolean);
    }

    @CacheEvict(value = "mailStartTlsEnable", allEntries = true)
    public void setMailStartTlsEnable(boolean value) {
        setConfigValue("spring.mail.properties.mail.smtp.starttls.enable", value, null);
    }
}
