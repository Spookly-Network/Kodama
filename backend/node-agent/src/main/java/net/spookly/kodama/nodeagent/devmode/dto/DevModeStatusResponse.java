package net.spookly.kodama.nodeagent.devmode.dto;

public record DevModeStatusResponse(boolean devMode, boolean previousDevMode, boolean changed) {

    public static DevModeStatusResponse fromCurrent(boolean current) {
        return new DevModeStatusResponse(current, current, false);
    }

    public static DevModeStatusResponse fromUpdate(boolean previous, boolean current) {
        return new DevModeStatusResponse(current, previous, previous != current);
    }
}
