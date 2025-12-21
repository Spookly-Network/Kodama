package net.spookly.kodama.brain.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "node")
public class NodeProperties {

    @Min(1)
    private int heartbeatIntervalSeconds = 30;

    @Min(1)
    private int commandTimeoutSeconds = 10;

    @Min(1)
    private int commandMaxAttempts = 2;

    @Min(0)
    private long commandRetryBackoffMillis = 500;

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }

    public void setCommandTimeoutSeconds(int commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }

    public int getCommandMaxAttempts() {
        return commandMaxAttempts;
    }

    public void setCommandMaxAttempts(int commandMaxAttempts) {
        this.commandMaxAttempts = commandMaxAttempts;
    }

    public long getCommandRetryBackoffMillis() {
        return commandRetryBackoffMillis;
    }

    public void setCommandRetryBackoffMillis(long commandRetryBackoffMillis) {
        this.commandRetryBackoffMillis = commandRetryBackoffMillis;
    }
}
