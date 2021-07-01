package org.jibe77.hermanas.controller.energy;

import java.time.LocalDate;

// * current mode
// * info about eco mode
//    * starting date
//    * ending date
//    * days before & after winter solstice
// * same for sunny mode
// * same for regular mode
public class EnergyMode {
    private String currentMode;
    private LocalDate ecoModeStartDate;
    private LocalDate ecoModeEndDate;
    private int ecoModeDaysAroundWinterSolstice;
    private LocalDate sunnyModeStartDate;
    private LocalDate sunnyModeEndDate;
    private int sunnyModeDaysAroundSummerSolstice;

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(String currentMode) {
        this.currentMode = currentMode;
    }

    public LocalDate getEcoModeStartDate() {
        return ecoModeStartDate;
    }

    public void setEcoModeStartDate(LocalDate ecoModeStartDate) {
        this.ecoModeStartDate = ecoModeStartDate;
    }

    public LocalDate getEcoModeEndDate() {
        return ecoModeEndDate;
    }

    public void setEcoModeEndDate(LocalDate ecoModeEndDate) {
        this.ecoModeEndDate = ecoModeEndDate;
    }

    public int getEcoModeDaysAroundWinterSolstice() {
        return ecoModeDaysAroundWinterSolstice;
    }

    public void setEcoModeDaysAroundWinterSolstice(int ecoModeDaysAroundWinterSolstice) {
        this.ecoModeDaysAroundWinterSolstice = ecoModeDaysAroundWinterSolstice;
    }

    public LocalDate getSunnyModeStartDate() {
        return sunnyModeStartDate;
    }

    public void setSunnyModeStartDate(LocalDate sunnyModeStartDate) {
        this.sunnyModeStartDate = sunnyModeStartDate;
    }

    public LocalDate getSunnyModeEndDate() {
        return sunnyModeEndDate;
    }

    public void setSunnyModeEndDate(LocalDate sunnyModeEndDate) {
        this.sunnyModeEndDate = sunnyModeEndDate;
    }

    public int getSunnyModeDaysAroundSummerSolstice() {
        return sunnyModeDaysAroundSummerSolstice;
    }

    public void setSunnyModeDaysAroundSummerSolstice(int sunnyModeDaysAroundSummerSolstice) {
        this.sunnyModeDaysAroundSummerSolstice = sunnyModeDaysAroundSummerSolstice;
    }
}
