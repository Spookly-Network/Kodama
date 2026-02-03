package net.spookly.kodama.nodeagent.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "instances")
@Getter
@Setter
public class InstanceProperties {

    private InstanceRuntime instanceRuntime = new InstanceRuntime();
    private InstanceCallbacks instanceCallbacks = new InstanceCallbacks();
    private InstanceMonitor instanceMonitor = new InstanceMonitor();

    public void validate() {
        List<String> errors = new ArrayList<>();

        if (instanceRuntime == null) {
            errors.add("instances.instance-runtime is required");
        } else {
            Integer stopTimeoutSeconds = instanceRuntime.getStopTimeoutSeconds();
            if (stopTimeoutSeconds != null && stopTimeoutSeconds < 0) {
                errors.add("instances.instance-runtime.stop-timeout-seconds must be 0 or greater");
            }
        }
        if (instanceCallbacks == null) {
            errors.add("instances.instance-callbacks is required");
        } else {
            if (instanceCallbacks.getMaxAttempts() < 1) {
                errors.add("instances.instance-callbacks.max-attempts must be at least 1");
            }
            if (instanceCallbacks.getRetryBackoffMillis() < 0) {
                errors.add("instances.instance-callbacks.retry-backoff-millis must be 0 or greater");
            }
        }
        if (instanceMonitor == null) {
            errors.add("instances.instance-monitor is required");
        } else {
            if (instanceMonitor.getIntervalSeconds() < 0) {
                errors.add("instances.instance-monitor.interval-seconds must be 0 or greater");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid node configuration:\n- " + String.join("\n- ", errors));
        }
    }

    @Getter
    @Setter
    public static class InstanceRuntime {
        private String image;
        private String workspaceMountPath = "/workspace";
        private String workingDir;
        private Integer stopTimeoutSeconds;
    }

    @Getter
    @Setter
    public static class InstanceCallbacks {
        private int maxAttempts = 1;
        private long retryBackoffMillis;
    }

    @Getter
    @Setter
    public static class InstanceMonitor {
        private boolean enabled = true;
        private int intervalSeconds = 10;
    }
}
