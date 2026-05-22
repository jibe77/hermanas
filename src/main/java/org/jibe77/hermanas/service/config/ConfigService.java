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

    @Value("${consumption.mode.eco.days.around.winter.solstice}")
    private int ecoModeNbrDaysAroundWinterSolstice;

    @Value("${consumption.mode.sunny.days.around.summer.solstice}")
    private int sunnyModeNbrDaysAroundSummerSolstice;

    @Value("${consumption.mode.eco.force}")
    private boolean consumptionModeEcoForce;

    @Value("${machine.shutdown.eco}")
    boolean machineShutdownInEcoMode;

    @Value("${wifi.disabled.eco}")
    boolean wifiDisabledInEcoMode;

    @Value("${machine.shutdown.sunny}")
    boolean machineShutdownInSunnyMode;

    @Value("${wifi.disabled.sunny}")
    boolean wifiDisabledInSunnyMode;

    @Value("${machine.shutdown.regular}")
    boolean machineShutdownInRegularMode;

    @Value("${wifi.disabled.regular}")
    boolean wifiDisabledInRegularMode;

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
     * Gets the number of days around winter solstice to use eco mode.
     * E.g., value of 30 means 30 days before and 30 days after solstice.
     *
     * @return number of days (each side of solstice)
     */
    @Cacheable(value = {"ecoModeNbrDaysAroundWinterSolstice"})
    public int getEcoModeNbrDaysAroundWinterSolstice() {
        return getConfigValue("consumption.mode.eco.days.around.winter.solstice",
                             ecoModeNbrDaysAroundWinterSolstice, Integer::parseInt);
    }

    /**
     * Sets the number of days around winter solstice to use eco mode.
     *
     * @param ecoModeNbrDaysAroundWinterSolstice number of days (must be positive)
     * @throws IllegalArgumentException if value is <= 0
     */
    @CacheEvict(value = "ecoModeNbrDaysAroundWinterSolstice")
    public void setEcoModeNbrDaysAroundWinterSolstice(int ecoModeNbrDaysAroundWinterSolstice) {
        setConfigValue("consumption.mode.eco.days.around.winter.solstice",
                      ecoModeNbrDaysAroundWinterSolstice, positiveIntValidator());
    }

    /**
     * Gets the number of days around summer solstice to use sunny mode.
     *
     * @return number of days (each side of solstice)
     */
    @Cacheable(value = {"sunnyModeNbrDaysAroundSummerSolstice"})
    public int getSunnyModeNbrDaysAroundSummerSolstice() {
        return getConfigValue("consumption.mode.sunny.days.around.summer.solstice",
                             sunnyModeNbrDaysAroundSummerSolstice, Integer::parseInt);
    }

    /**
     * Sets the number of days around summer solstice to use sunny mode.
     *
     * @param sunnyModeNbrDaysAroundSummerSolstice number of days (must be positive)
     * @throws IllegalArgumentException if value is <= 0
     */
    @CacheEvict(value = "sunnyModeNbrDaysAroundSummerSolstice")
    public void setSunnyModeNbrDaysAroundSummerSolstice(int sunnyModeNbrDaysAroundSummerSolstice) {
        setConfigValue("consumption.mode.sunny.days.around.summer.solstice",
                      sunnyModeNbrDaysAroundSummerSolstice, positiveIntValidator());
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
     * Checks if machine should shut down in eco mode.
     *
     * @return true if shutdown enabled in eco mode
     */
    @Cacheable(value = {"machineShutdownInEcoMode"})
    public boolean isMachineShutdownInEcoMode() {
        return getConfigValue("machine.shutdown.eco", machineShutdownInEcoMode, Boolean::valueOf);
    }

    /**
     * Sets whether machine should shut down in eco mode.
     *
     * @param machineShutdownInEcoMode true to enable shutdown
     */
    @CacheEvict(value = "machineShutdownInEcoMode")
    public void setMachineShutdownInEcoMode(boolean machineShutdownInEcoMode) {
        setConfigValue("machine.shutdown.eco", machineShutdownInEcoMode, null);
    }

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
     * Checks if machine should shut down in sunny mode.
     *
     * @return true if shutdown enabled in sunny mode
     */
    @Cacheable(value = {"machineShutdownInSunnyMode"})
    public boolean isMachineShutdownInSunnyMode() {
        return getConfigValue("machine.shutdown.sunny", machineShutdownInSunnyMode, Boolean::valueOf);
    }

    /**
     * Sets whether machine should shut down in sunny mode.
     *
     * @param machineShutdownInSunnyMode true to enable shutdown
     */
    @CacheEvict(value = "machineShutdownInSunnyMode")
    public void setMachineShutdownInSunnyMode(boolean machineShutdownInSunnyMode) {
        setConfigValue("machine.shutdown.sunny", machineShutdownInSunnyMode, null);
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
     * Checks if machine should shut down in regular mode.
     *
     * @return true if shutdown enabled in regular mode
     */
    @Cacheable(value = {"machineShutdownInRegularMode"})
    public boolean isMachineShutdownInRegularMode() {
        return getConfigValue("machine.shutdown.regular", machineShutdownInRegularMode, Boolean::valueOf);
    }

    /**
     * Sets whether machine should shut down in regular mode.
     *
     * @param machineShutdownInRegularMode true to enable shutdown
     */
    @CacheEvict(value = "machineShutdownInRegularMode")
    public void setMachineShutdownInRegularMode(boolean machineShutdownInRegularMode) {
        setConfigValue("machine.shutdown.regular", machineShutdownInRegularMode, null);
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
}
