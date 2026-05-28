package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.energy.EnergyMode;
import org.jibe77.hermanas.service.energy.EnergyModeConfig;
import org.jibe77.hermanas.service.energy.EnergyModeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Picks the active energy mode based on a configurable month → mode mapping.
 *
 * <p>The previous solstice-based logic was replaced with a 12-entry monthly schedule
 * that an administrator can edit at runtime. {@link ConfigService#isConsumptionModeEcoForce()}
 * still overrides everything to ECO when set.</p>
 */
@Component
public class ConsumptionModeController {

    ConfigService configService;

    public ConsumptionModeController(ConfigService configService) {
        this.configService = configService;
    }

    public long getDuration(long ecoModeDuration, long regularModeDuration, long sunnyModeDuration, LocalDateTime time) {
        switch (getCurrentMode(time)) {
            case ECO:
                return ecoModeDuration;
            case SUNNY:
                return sunnyModeDuration;
            default:
                return regularModeDuration;
        }
    }

    private EnergyModeEnum getCurrentMode(LocalDateTime time) {
        if (configService.isConsumptionModeEcoForce()) {
            return EnergyModeEnum.ECO;
        }
        return configService.getMonthMode(time.getMonthValue());
    }

    /**
     * Convenience: true if the active mode at the given time is ECO. Kept because
     * several schedulers (door, fan, light) branch on the ECO mode.
     */
    public boolean isEcoMode(LocalDateTime time) {
        return getCurrentMode(time) == EnergyModeEnum.ECO;
    }

    public EnergyMode getCurrentEnergyMode(LocalDateTime time) {
        EnergyMode energyMode = new EnergyMode();
        energyMode.setCurrentMode(getCurrentMode(time).name());
        energyMode.setForced(configService.isConsumptionModeEcoForce());
        energyMode.setMonthlyMapping(readMonthlyMapping());
        return energyMode;
    }

    public EnergyMode getCurrentEnergyMode() {
        return getCurrentEnergyMode(LocalDateTime.now());
    }

    /**
     * Replaces the previous 12 individual setters with a single bulk update so the
     * admin UI can save the whole calendar atomically.
     */
    public void updateMonthlyMapping(Map<Integer, EnergyModeEnum> mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("mapping must not be null");
        }
        for (int month = 1; month <= 12; month++) {
            EnergyModeEnum mode = mapping.get(month);
            if (mode != null) {
                configService.setMonthMode(month, mode);
            }
        }
    }

    private Map<Integer, EnergyModeEnum> readMonthlyMapping() {
        Map<Integer, EnergyModeEnum> mapping = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            mapping.put(month, configService.getMonthMode(month));
        }
        return mapping;
    }

    public EnergyModeConfig getEnergyModeConfig(String energyMode) {
        EnergyModeConfig energyModeConfig = new EnergyModeConfig();
        EnergyModeEnum energyModeEnum = EnergyModeEnum.valueOf(energyMode);
        energyModeConfig.setEnergyMode(energyModeEnum);
        switch (energyModeEnum) {
            case ECO:
                energyModeConfig.setDurationOfFanInMilliseconds(configService.getFanSecurityTimerDelayEco());
                energyModeConfig.setDurationOfLightInMilliseconds(configService.getLightSecurityTimerDelayEco());
                energyModeConfig.setDurationOfMusicInMilliseconds(configService.getMusicSecurityTimerDelayEco());
                energyModeConfig.setMachineShutdown(configService.isMachineShutdownInEcoMode());
                energyModeConfig.setWifiDisabled(configService.isWifiDisabledInEcoMode());
                break;
            case SUNNY:
                energyModeConfig.setDurationOfFanInMilliseconds(configService.getFanSecurityTimerDelaySunny());
                energyModeConfig.setDurationOfLightInMilliseconds(configService.getLightSecurityTimerDelaySunny());
                energyModeConfig.setDurationOfMusicInMilliseconds(configService.getMusicSecurityTimerDelaySunny());
                energyModeConfig.setMachineShutdown(configService.isMachineShutdownInSunnyMode());
                energyModeConfig.setWifiDisabled(configService.isWifiDisabledInSunnyMode());
                break;
            case REGULAR:
                energyModeConfig.setDurationOfFanInMilliseconds(configService.getFanSecurityTimerDelayRegular());
                energyModeConfig.setDurationOfLightInMilliseconds(configService.getLightSecurityTimerDelayRegular());
                energyModeConfig.setDurationOfMusicInMilliseconds(configService.getMusicSecurityTimerDelayRegular());
                energyModeConfig.setMachineShutdown(configService.isMachineShutdownInRegularMode());
                energyModeConfig.setWifiDisabled(configService.isWifiDisabledInRegularMode());
                break;
        }
        return energyModeConfig;
    }

    public EnergyModeConfig updateEnergyModeConfig(EnergyModeConfig energyModeConfig) {
        switch (energyModeConfig.getEnergyMode()) {
            case ECO:
                configService.setFanSecurityTimerDelayEco(energyModeConfig.getDurationOfFanInMilliseconds());
                configService.setLightSecurityTimerDelayEco(energyModeConfig.getDurationOfLightInMilliseconds());
                configService.setMusicSecurityTimerDelayEco(energyModeConfig.getDurationOfMusicInMilliseconds());
                configService.setMachineShutdownInEcoMode(energyModeConfig.isMachineShutdown());
                // wifi.disabled.* is intentionally not exposed to the admin UI — see
                // application.properties. We do not propagate the incoming flag.
                break;
            case SUNNY:
                configService.setFanSecurityTimerDelaySunny(energyModeConfig.getDurationOfFanInMilliseconds());
                configService.setLightSecurityTimerDelaySunny(energyModeConfig.getDurationOfLightInMilliseconds());
                configService.setMusicSecurityTimerDelaySunny(energyModeConfig.getDurationOfMusicInMilliseconds());
                configService.setMachineShutdownInSunnyMode(energyModeConfig.isMachineShutdown());
                break;
            case REGULAR:
                configService.setFanSecurityTimerDelayRegular(energyModeConfig.getDurationOfFanInMilliseconds());
                configService.setLightSecurityTimerDelayRegular(energyModeConfig.getDurationOfLightInMilliseconds());
                configService.setMusicSecurityTimerDelayRegular(energyModeConfig.getDurationOfMusicInMilliseconds());
                configService.setMachineShutdownInRegularMode(energyModeConfig.isMachineShutdown());
                break;
        }
        return energyModeConfig;
    }

    public EnergyModeConfig getCurrentConfigMode() {
        return getEnergyModeConfig(getCurrentEnergyMode().getCurrentMode());
    }

    /**
     * Switches the global "force ECO" flag.
     */
    public void setEcoForced(boolean forced) {
        configService.setConsumptionModeEcoForce(forced);
    }

    /**
     * Used by the legacy {@code /api/v1/energy/dateRange} endpoint. Kept temporarily
     * so the existing REST surface compiles, but the calculation is now a no-op:
     * the monthly mapping has no concept of "days around solstice". Returns the
     * current mode unchanged.
     */
    public EnergyMode getCurrentEnergyMode(LocalDateTime time,
                                           @SuppressWarnings("unused") int unusedDaysWinter,
                                           @SuppressWarnings("unused") int unusedDaysSummer) {
        return getCurrentEnergyMode(time);
    }
}
