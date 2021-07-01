package org.jibe77.hermanas.controller.energy;

public class EnergyModeConfig {
    private EnergyModeEnum energyMode;
    private boolean machineShutdown;
    private boolean wifiDisabled;
    private long durationOfFanInMilliseconds;
    private long durationOfLightInMilliseconds;
    private long durationOfMusicInMilliseconds;

    public EnergyModeEnum getEnergyMode() {
        return energyMode;
    }

    public void setEnergyMode(EnergyModeEnum energyMode) {
        this.energyMode = energyMode;
    }

    public boolean isMachineShutdown() {
        return machineShutdown;
    }

    public void setMachineShutdown(boolean machineShutdown) {
        this.machineShutdown = machineShutdown;
    }

    public boolean isWifiDisabled() {
        return wifiDisabled;
    }

    public void setWifiDisabled(boolean wifiDisabled) {
        this.wifiDisabled = wifiDisabled;
    }

    public long getDurationOfFanInMilliseconds() {
        return durationOfFanInMilliseconds;
    }

    public void setDurationOfFanInMilliseconds(long durationOfFanInMilliseconds) {
        this.durationOfFanInMilliseconds = durationOfFanInMilliseconds;
    }

    public long getDurationOfLightInMilliseconds() {
        return durationOfLightInMilliseconds;
    }

    public void setDurationOfLightInMilliseconds(long durationOfLightInMilliseconds) {
        this.durationOfLightInMilliseconds = durationOfLightInMilliseconds;
    }

    public long getDurationOfMusicInMilliseconds() {
        return durationOfMusicInMilliseconds;
    }

    public void setDurationOfMusicInMilliseconds(long durationOfMusicInMilliseconds) {
        this.durationOfMusicInMilliseconds = durationOfMusicInMilliseconds;
    }
}
