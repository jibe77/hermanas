package org.jibe77.hermanas.scheduler.sun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConsumptionModeManager {

    @Value("${consumption.mode.eco.force}")
    private boolean consumptionModeEcoForce;

    public long getDuration(long ecoModeDuration, long regularModeDuration, long sunnyModeDuration) {
        if (isEcoMode(LocalDateTime.now())) {
            return ecoModeDuration;
        } else if (isSunnyMode(LocalDateTime.now())) {
            return sunnyModeDuration;
        } else {
            return regularModeDuration;
        }
    }

    public boolean isEcoMode() {
        if (consumptionModeEcoForce) {
            return true;
        } else {
            return isEcoMode(LocalDateTime.now());
        }
    }

    /**
     * Eco mode is between 1st of december to 15th of january.
     * @param time
     * @return true if currently in eco mode.
     */
    public boolean isEcoMode(LocalDateTime time) {
        switch(time.getMonthValue()) {
            case 11:
                return time.getDayOfMonth() > 20;
            case 12:
            case 1:
                return true;
            default:
                return false;
        }
    }

    public boolean isSunnyMode(LocalDateTime time) {
        switch (time.getMonthValue()) {
            case 5:
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }
}
