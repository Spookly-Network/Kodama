package net.spookly.kodama.brain.plugin;

public interface KodamaBrainPlugin {

    int API_VERSION = 1;

    String id();

    default int apiVersion() {
        return API_VERSION;
    }

    default String displayName() {
        return id();
    }

    default BrainPrepareInstanceMutation onPrepareInstance(BrainPrepareInstanceContext context) {
        return BrainPrepareInstanceMutation.empty();
    }
}
