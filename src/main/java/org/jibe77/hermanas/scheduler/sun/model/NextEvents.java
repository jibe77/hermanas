package org.jibe77.hermanas.scheduler.sun.model;

import java.time.LocalDateTime;

public class NextEvents {

    LocalDateTime nextDoorOpeningTime;
    LocalDateTime nextLightOnTime;
    LocalDateTime nextDoorClosingTime;

    public NextEvents(LocalDateTime nextDoorOpeningTime, LocalDateTime nextLightOnTime, LocalDateTime nextDoorClosingTime) {
        this.nextDoorOpeningTime = nextDoorOpeningTime;
        this.nextLightOnTime = nextLightOnTime;
        this.nextDoorClosingTime = nextDoorClosingTime;
    }

    public LocalDateTime getNextDoorOpeningTime() {
        return nextDoorOpeningTime;
    }

    public void setNextDoorOpeningTime(LocalDateTime nextDoorOpeningTime) {
        this.nextDoorOpeningTime = nextDoorOpeningTime;
    }

    public LocalDateTime getNextLightOnTime() {
        return nextLightOnTime;
    }

    public void setNextLightOnTime(LocalDateTime nextLightOnTime) {
        this.nextLightOnTime = nextLightOnTime;
    }

    public LocalDateTime getNextDoorClosingTime() {
        return nextDoorClosingTime;
    }

    public void setNextDoorClosingTime(LocalDateTime nextDoorClosingTime) {
        this.nextDoorClosingTime = nextDoorClosingTime;
    }

    @Override
    public String toString() {
        return "NextEvents{" +
                "nextDoorOpeningTime=" + nextDoorOpeningTime +
                ", nextLightOnTime=" + nextLightOnTime +
                ", nextDoorClosingTime=" + nextDoorClosingTime +
                '}';
    }
}
