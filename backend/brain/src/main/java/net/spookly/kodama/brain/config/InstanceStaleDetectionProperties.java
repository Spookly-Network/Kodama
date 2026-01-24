package net.spookly.kodama.brain.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "instance.stale-detection")
public class InstanceStaleDetectionProperties {

    private boolean enabled = true;

    @Min(1)
    private int monitorIntervalSeconds = 60;

    @Min(1)
    private int preparingTimeoutSeconds = 300;

    @Min(1)
    private int startingTimeoutSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }

    public int getPreparingTimeoutSeconds() {
        return preparingTimeoutSeconds;
    }

    public void setPreparingTimeoutSeconds(int preparingTimeoutSeconds) {
        this.preparingTimeoutSeconds = preparingTimeoutSeconds;
    }

    public int getStartingTimeoutSeconds() {
        return startingTimeoutSeconds;
    }

    public void setStartingTimeoutSeconds(int startingTimeoutSeconds) {
        this.startingTimeoutSeconds = startingTimeoutSeconds;
    }
}
