package net.spookly.kodama.brain.domain.instance;

public enum InstanceState {
    REQUESTED,
    PREPARING,
    PREPARED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    DESTROYED,
    FAILED
}
