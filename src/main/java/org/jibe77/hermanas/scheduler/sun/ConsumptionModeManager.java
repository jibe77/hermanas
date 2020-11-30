package org.jibe77.hermanas.scheduler.sun;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConsumptionModeManager {

    public long getDuration(long ecoModeDuration, long regularModeDuration, long sunnyModeDuration) {
        int monthValue = LocalDateTime.now().getMonthValue();
        if (isEcoMode(monthValue)) {
            return ecoModeDuration;
        } else if (isSunnyMode(monthValue)) {
            return sunnyModeDuration;
        } else {
            return regularModeDuration;
        }
    }

    public boolean isEcoMode() {
        return isEcoMode(LocalDateTime.now().getMonthValue());
    }

    public boolean isEcoMode(int monthValue) {
        switch(monthValue) {
            case 1:
            case 11:
            case 12:
                return true;
            default:
                return false;
        }
    }

    public boolean isSunnyMode(int monthValue) {
        switch (monthValue) {
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
