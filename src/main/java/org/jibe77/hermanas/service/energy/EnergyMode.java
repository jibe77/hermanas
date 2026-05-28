package org.jibe77.hermanas.service.energy;

import java.util.Map;

/**
 * Snapshot of the energy mode state exposed by the REST API.
 *
 * <p>The previous solstice-based fields (start/end dates, days around solstice)
 * are gone — the mode now follows a 12-entry month → mode mapping that the admin
 * can edit at runtime.</p>
 */
public class EnergyMode {
    private String currentMode;
    private boolean forced;
    private Map<Integer, EnergyModeEnum> monthlyMapping;

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(String currentMode) {
        this.currentMode = currentMode;
    }

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public Map<Integer, EnergyModeEnum> getMonthlyMapping() {
        return monthlyMapping;
    }

    public void setMonthlyMapping(Map<Integer, EnergyModeEnum> monthlyMapping) {
        this.monthlyMapping = monthlyMapping;
    }
}
