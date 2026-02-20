package net.spookly.kodama.nodeagent.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InstancePropertiesTest {

    @Test
    void validateRejectsNonPositiveInstallScriptTimeout() {
        InstanceProperties properties = new InstanceProperties();
        properties.getInstanceRuntime().setInstallScriptTimeoutSeconds(0);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("instances.instance-runtime.install-script-timeout-seconds must be greater than 0");
    }

    @Test
    void validateRejectsNegativeStopTimeout() {
        InstanceProperties properties = new InstanceProperties();
        properties.getInstanceRuntime().setStopTimeoutSeconds(-1);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("instances.instance-runtime.stop-timeout-seconds must be 0 or greater");
    }

    @Test
    void validateRejectsNegativeInstanceMonitorInterval() {
        InstanceProperties properties = new InstanceProperties();
        properties.getInstanceMonitor().setIntervalSeconds(-1);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("instances.instance-monitor.interval-seconds must be 0 or greater");
    }
}
