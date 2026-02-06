package net.spookly.kodama.nodeagent.devmode.service;

import java.util.concurrent.atomic.AtomicBoolean;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.stereotype.Service;

@Service
public class DevModeService {

    private final AtomicBoolean devMode;

    public DevModeService(NodeConfig config) {
        boolean initial = config != null && config.isDevMode();
        this.devMode = new AtomicBoolean(initial);
    }

    public boolean isDevModeEnabled() {
        return devMode.get();
    }

    public boolean setDevMode(boolean enabled) {
        return devMode.getAndSet(enabled);
    }
}
