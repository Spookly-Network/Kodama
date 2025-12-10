package net.spookly.kodama.brain.domain.instance;

public enum InstanceState {
    REQUESTED,
    PREPARING,
    STARTING,
    RUNNING,
    STOPPING,
    DESTROYED,
    FAILED
}
