package org.jibe77.hermanas.websocket;

import org.jibe77.hermanas.controller.abstract_model.StatusEnum;

public class CoopStatus {

    private Appliance appliance;
    private StatusEnum state;

    public CoopStatus(Appliance appliance, StatusEnum state) {
        this.appliance = appliance;
        this.state = state;
    }

    public Appliance getAppliance() {
        return appliance;
    }

    public void setAppliance(Appliance appliance) {
        this.appliance = appliance;
    }

    public StatusEnum getState() {
        return state;
    }

    public void setState(StatusEnum state) {
        this.state = state;
    }
}
