package org.jibe77.hermanas.image;

public class PredictionException extends RuntimeException {

    final DoorStatus doorStatus;

    public PredictionException(DoorStatus doorStatus) {
        this.doorStatus = doorStatus;
    }

    public DoorStatus getDoorStatus() {
        return doorStatus;
    }
}
