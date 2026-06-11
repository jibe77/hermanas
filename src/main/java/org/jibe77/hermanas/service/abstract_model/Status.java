package org.jibe77.hermanas.service.abstract_model;

public class Status {
    private final StatusEnum status;
    private final int timeOut;

    public Status(StatusEnum status, int timeOut) {
        this.status = status;
        this.timeOut = timeOut;
    }

    public StatusEnum getStatusEnum() {
        return status;
    }

    public int getTimeOut() {
        return timeOut;
    }

    @Override
    public String toString() {
        return "Status{" +
                "status=" + status +
                ", timeOut=" + timeOut +
                '}';
    }
}
