package org.jibe77.hermanas.websocket;

public class ButtonStatus {

    private Button button;
    private boolean pressed;
    private long timestamp;

    public ButtonStatus() {
    }

    public ButtonStatus(Button button, boolean pressed, long timestamp) {
        this.button = button;
        this.pressed = pressed;
        this.timestamp = timestamp;
    }

    public Button getButton() {
        return button;
    }

    public void setButton(Button button) {
        this.button = button;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
