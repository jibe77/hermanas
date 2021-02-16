package org.jibe77.hermanas.controller.abstract_model;

public class Status {
    StatusEnum status;
    int timeOut;

    public Status(StatusEnum status, int timeOut) {
        this.status = status;
        this.timeOut = timeOut;
    }

    public StatusEnum getStatusEnum() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public String toString() {
        return "FanStatus{" +
                "status=" + status +
                ", timeOut=" + timeOut +
                '}';
    }
}
