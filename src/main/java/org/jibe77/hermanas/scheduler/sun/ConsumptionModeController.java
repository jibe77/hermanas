package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.controller.config.ConfigController;
import org.jibe77.hermanas.controller.energy.EnergyMode;
import org.jibe77.hermanas.controller.energy.EnergyModeConfig;
import org.jibe77.hermanas.controller.energy.EnergyModeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConsumptionModeController {

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

    ConfigController configController;

    public ConsumptionModeController(ConfigController configController) {
        this.configController = configController;
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
        int ecoModeNbrDaysAroundWinterSolstice = configController.getEcoModeNbrDaysAroundWinterSolstice();
        if (consumptionModeEcoForce|| time.getDayOfYear() < (ecoModeNbrDaysAroundWinterSolstice-10)) {
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
        int sunnyModeNbrDaysAroundSummerSolstice = configController.getSunnyModeNbrDaysAroundSummerSolstice();
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
        return getCurrentEnergyMode(time, configController.getEcoModeNbrDaysAroundWinterSolstice(), configController.getSunnyModeNbrDaysAroundSummerSolstice());
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
        if (now.isBefore(lastYearWinterSolstice.plusDays(configController.getEcoModeNbrDaysAroundWinterSolstice()))) {
            return getWinterSolsticeDay(now.getYear() -1);
        }
        LocalDateTime nextWinterSolstice = getWinterSolsticeDay(now.getYear());
        if (now.isAfter(nextWinterSolstice.plusDays(configController.getEcoModeNbrDaysAroundWinterSolstice()))) {
            return getWinterSolsticeDay(now.getYear()-1);
        }
        return nextWinterSolstice;
    }

    protected LocalDateTime getSummerSolstice(LocalDateTime now) {
        LocalDateTime nextSummerSolstice = getSummerSolsticeDay(now.getYear());
        if (now.isAfter(nextSummerSolstice.plusDays(configController.getSunnyModeNbrDaysAroundSummerSolstice()))) {
            return getSummerSolsticeDay(now.getYear()+1);
        }
        return nextSummerSolstice;
    }

    public boolean isConsumptionModeEcoForce() {
        return consumptionModeEcoForce;
    }

    protected void setConsumptionModeEcoForce(boolean consumptionModeEcoForce) {
        this.consumptionModeEcoForce = consumptionModeEcoForce;
    }

    public EnergyModeConfig getEnergyModeConfig(EnergyModeEnum energyModeEnum) {
        EnergyModeConfig energyModeConfig = new EnergyModeConfig();
        energyModeConfig.setEnergyMode(energyModeEnum);
        switch (energyModeEnum) {
            case ECO:
                energyModeConfig.setDurationOfFanInMilliseconds(configController.getFanSecurityTimerDelayEco());
                energyModeConfig.setDurationOfLightInMilliseconds(configController.getLightSecurityTimerDelayEco());
                energyModeConfig.setDurationOfMusicInMilliseconds(configController.getMusicSecurityTimerDelayEco());
                energyModeConfig.setMachineShutdown(machineShutdownInEcoMode);
                energyModeConfig.setWifiDisabled(wifiDisabledInEcoMode);
                break;
            case SUNNY:
                energyModeConfig.setDurationOfFanInMilliseconds(configController.getFanSecurityTimerDelaySunny());
                energyModeConfig.setDurationOfLightInMilliseconds(configController.getLightSecurityTimerDelaySunny());
                energyModeConfig.setDurationOfMusicInMilliseconds(configController.getMusicSecurityTimerDelaySunny());
                energyModeConfig.setMachineShutdown(machineShutdownInSunnyMode);
                energyModeConfig.setWifiDisabled(wifiDisabledInSunnyMode);
                break;
            case REGULAR:
                energyModeConfig.setDurationOfFanInMilliseconds(configController.getFanSecurityTimerDelayRegular());
                energyModeConfig.setDurationOfLightInMilliseconds(configController.getLightSecurityTimerDelayRegular());
                energyModeConfig.setDurationOfMusicInMilliseconds(configController.getMusicSecurityTimerDelayRegular());
                energyModeConfig.setMachineShutdown(machineShutdownInRegularMode);
                energyModeConfig.setWifiDisabled(wifiDisabledInRegularMode);
                break;
        }
        return energyModeConfig;
    }

    public EnergyModeConfig updateEnergyModeConfig(EnergyModeConfig energyModeConfig) {
        // TODO
        return energyModeConfig;
    }
}
