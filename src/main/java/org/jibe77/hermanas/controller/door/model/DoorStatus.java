package org.jibe77.hermanas.controller.door.model;

import java.time.LocalDateTime;

public class DoorStatus {

    DoorStatusEnum status;
    LocalDateTime timeStatusHasChanged;

    public DoorStatus(DoorStatusEnum status, LocalDateTime timeStatusHasChanged) {
        this.status = status;
        this.timeStatusHasChanged = timeStatusHasChanged;
    }

    public DoorStatusEnum getStatus() {
        return status;
    }

    public void setStatus(DoorStatusEnum status) {
        this.status = status;
    }

    public LocalDateTime getTimeStatusHasChanged() {
        return timeStatusHasChanged;
    }

    public void setTimeStatusHasChanged(LocalDateTime timeStatusHasChanged) {
        this.timeStatusHasChanged = timeStatusHasChanged;
    }

    @Override
    public String toString() {
        return "DoorStatus{" +
                "status=" + status +
                ", timeStatusHasChanged=" + timeStatusHasChanged +
                '}';
    }
}
