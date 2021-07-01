package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.controller.energy.EnergyMode;
import org.jibe77.hermanas.controller.energy.EnergyModeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConsumptionModeController {

    @Value("${consumption.mode.eco.days.around.winter.solstice}")
    private int ecoModeNbrDaysAroundWinterSolstice;

    @Value("${consumption.mode.sunny.days.around.summer.solstice}")
    private int sunnyModeNbrDaysAroundSummerSolstice;

    @Value("${consumption.mode.eco.force}")
    private boolean consumptionModeEcoForce;

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
        if (now.isBefore(lastYearWinterSolstice.plusDays(ecoModeNbrDaysAroundWinterSolstice))) {
            return getWinterSolsticeDay(now.getYear() -1);
        }
        LocalDateTime nextWinterSolstice = getWinterSolsticeDay(now.getYear());
        if (now.isAfter(nextWinterSolstice.plusDays(ecoModeNbrDaysAroundWinterSolstice))) {
            return getWinterSolsticeDay(now.getYear()-1);
        }
        return nextWinterSolstice;
    }

    protected LocalDateTime getSummerSolstice(LocalDateTime now) {
        LocalDateTime nextSummerSolstice = getSummerSolsticeDay(now.getYear());
        if (now.isAfter(nextSummerSolstice.plusDays(sunnyModeNbrDaysAroundSummerSolstice))) {
            return getSummerSolsticeDay(now.getYear()+1);
        }
        return nextSummerSolstice;
    }

    protected void setEcoModeNbrDaysAroundWinterSolstice(int ecoModeNbrDaysAroundWinterSolstice) {
        this.ecoModeNbrDaysAroundWinterSolstice = ecoModeNbrDaysAroundWinterSolstice;
    }

    protected void setSunnyModeNbrDaysAroundSummerSolstice(int sunnyModeNbrDaysAroundSummerSolstice) {
        this.sunnyModeNbrDaysAroundSummerSolstice = sunnyModeNbrDaysAroundSummerSolstice;
    }

    public boolean isConsumptionModeEcoForce() {
        return consumptionModeEcoForce;
    }

    protected void setConsumptionModeEcoForce(boolean consumptionModeEcoForce) {
        this.consumptionModeEcoForce = consumptionModeEcoForce;
    }
}
