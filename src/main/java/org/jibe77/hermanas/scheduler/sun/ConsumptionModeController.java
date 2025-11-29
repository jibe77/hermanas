package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.controller.config.ConfigService;
import org.jibe77.hermanas.service.energy.EnergyMode;
import org.jibe77.hermanas.service.energy.EnergyModeConfig;
import org.jibe77.hermanas.service.energy.EnergyModeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
        if (isEcoMode(time)) {
            return EnergyModeEnum.ECO;
        } else if (isSunnyMode(time)) {
            return EnergyModeEnum.SUNNY;
        } else {
            return EnergyModeEnum.REGULAR;
        }
    }

    /**
     * Eco mode is between 1st of december to 15th of january.
     * @param time
     * @return true if currently in eco mode.
     */
    public boolean isEcoMode(LocalDateTime time) {
        int ecoModeNbrDaysAroundWinterSolstice = configService.getEcoModeNbrDaysAroundWinterSolstice();
        if (configService.isConsumptionModeEcoForce() || time.getDayOfYear() < (ecoModeNbrDaysAroundWinterSolstice-10)) {
            return true;
        } else {
            int winterDay = getWinterSolsticeDay(time.getYear()).getDayOfYear();
            return time.getDayOfYear() >= (winterDay - ecoModeNbrDaysAroundWinterSolstice) &&
                    time.getDayOfYear() <= (winterDay + ecoModeNbrDaysAroundWinterSolstice);
        }
    }

    protected int getNumberOfDaysInYear(int year) {
        return LocalDateTime.of(
                year, 12, 31, 23, 59).getDayOfYear();
    }

    public boolean isSunnyMode(LocalDateTime time) {
        int summerDay = getSummerSolsticeDay(time.getYear()).getDayOfYear();
        int sunnyModeNbrDaysAroundSummerSolstice = configService.getSunnyModeNbrDaysAroundSummerSolstice();
        return time.getDayOfYear() >= (summerDay - sunnyModeNbrDaysAroundSummerSolstice) &&
                time.getDayOfYear() <= (summerDay + sunnyModeNbrDaysAroundSummerSolstice);
    }

    protected LocalDateTime getWinterSolsticeDay(int year) {
        return LocalDateTime.of(year, 12, 21, 12, 00);
    }

    protected LocalDateTime getSummerSolsticeDay(int year) {
        return LocalDateTime.of(year, 6, 21, 12, 00);
    }

    public EnergyMode getCurrentEnergyMode(LocalDateTime time) {
        return getCurrentEnergyMode(time, configService.getEcoModeNbrDaysAroundWinterSolstice(), configService.getSunnyModeNbrDaysAroundSummerSolstice());
    }

    public EnergyMode getCurrentEnergyMode() {
        return getCurrentEnergyMode(LocalDateTime.now(), configService.getEcoModeNbrDaysAroundWinterSolstice(), configService.getSunnyModeNbrDaysAroundSummerSolstice());
    }

    public EnergyMode getCurrentEnergyMode(LocalDateTime time, int ecoModeNbrDaysAroundWinterSolstice, int sunnyModeNbrDaysAroundSummerSolstice) {
        EnergyMode energyMode = new EnergyMode();
        energyMode.setCurrentMode(getCurrentMode(time).name());
        energyMode.setEcoModeDaysAroundWinterSolstice(ecoModeNbrDaysAroundWinterSolstice);
        energyMode.setEcoModeEndDate(getWinterSolstice(time).plusDays(ecoModeNbrDaysAroundWinterSolstice).toLocalDate());
        energyMode.setEcoModeStartDate(getWinterSolstice(time).minusDays(ecoModeNbrDaysAroundWinterSolstice).toLocalDate());
        energyMode.setSunnyModeDaysAroundSummerSolstice(sunnyModeNbrDaysAroundSummerSolstice);
        energyMode.setSunnyModeEndDate(getSummerSolstice(time).plusDays(sunnyModeNbrDaysAroundSummerSolstice).toLocalDate());
        energyMode.setSunnyModeStartDate(getSummerSolstice(time).minusDays(sunnyModeNbrDaysAroundSummerSolstice).toLocalDate());
        return energyMode;
    }

    protected LocalDateTime getWinterSolstice(LocalDateTime now) {
        LocalDateTime lastYearWinterSolstice = getWinterSolsticeDay(now.getYear()-1);
        if (now.isBefore(lastYearWinterSolstice.plusDays(configService.getEcoModeNbrDaysAroundWinterSolstice()))) {
            return getWinterSolsticeDay(now.getYear() -1);
        }
        LocalDateTime nextWinterSolstice = getWinterSolsticeDay(now.getYear());
        if (now.isAfter(nextWinterSolstice.plusDays(configService.getEcoModeNbrDaysAroundWinterSolstice()))) {
            return getWinterSolsticeDay(now.getYear()-1);
        }
        return nextWinterSolstice;
    }

    protected LocalDateTime getSummerSolstice(LocalDateTime now) {
        LocalDateTime nextSummerSolstice = getSummerSolsticeDay(now.getYear());
        if (now.isAfter(nextSummerSolstice.plusDays(configService.getSunnyModeNbrDaysAroundSummerSolstice()))) {
            return getSummerSolsticeDay(now.getYear()+1);
        }
        return nextSummerSolstice;
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
                configService.setWifiDisabledInEcoMode(energyModeConfig.isWifiDisabled());
                break;
            case SUNNY:
                configService.setFanSecurityTimerDelaySunny(energyModeConfig.getDurationOfFanInMilliseconds());
                configService.setLightSecurityTimerDelaySunny(energyModeConfig.getDurationOfLightInMilliseconds());
                configService.setMusicSecurityTimerDelaySunny(energyModeConfig.getDurationOfMusicInMilliseconds());
                configService.setMachineShutdownInSunnyMode(energyModeConfig.isMachineShutdown());
                configService.setWifiDisabledInSunnyMode(energyModeConfig.isWifiDisabled());
                break;
            case REGULAR:
                configService.setFanSecurityTimerDelayRegular(energyModeConfig.getDurationOfFanInMilliseconds());
                configService.setLightSecurityTimerDelayRegular(energyModeConfig.getDurationOfLightInMilliseconds());
                configService.setMusicSecurityTimerDelayRegular(energyModeConfig.getDurationOfMusicInMilliseconds());
                configService.setMachineShutdownInRegularMode(energyModeConfig.isMachineShutdown());
                configService.setWifiDisabledInRegularMode(energyModeConfig.isWifiDisabled());
                break;
        }
        return energyModeConfig;
    }

    public EnergyModeConfig getCurrentConfigMode() {
        return getEnergyModeConfig(getCurrentEnergyMode().getCurrentMode());
    }
}
