package net.spookly.kodama.nodeagent.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NodeConfigTest {

    @Test
    void validateFailsWhenRequiredConfigMissing() {
        NodeConfig config = new NodeConfig();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("node-agent.node-name is required")
                .hasMessageContaining("node-agent.node-version is required")
                .hasMessageContaining("node-agent.region is required")
                .hasMessageContaining("node-agent.brain-base-url is required")
                .hasMessageContaining("node-agent.cache-dir is required")
                .hasMessageContaining("node-agent.capacity-slots must be at least 1");
    }

    @Test
    void validateAcceptsRequiredConfig() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("Node 1");
        config.setNodeVersion("1.0.0");
        config.setRegion("local");
        config.setCapacitySlots(4);
        config.setBrainBaseUrl("http://brain:8080");
        config.setCacheDir("./cache");

        assertThatNoException().isThrownBy(config::validate);
    }

    @Test
    void validateRejectsNegativeHeartbeatInterval() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("Node 1");
        config.setNodeVersion("1.0.0");
        config.setRegion("local");
        config.setCapacitySlots(4);
        config.setBrainBaseUrl("http://brain:8080");
        config.setCacheDir("./cache");
        config.setHeartbeatIntervalSeconds(-1);

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("node-agent.heartbeat-interval-seconds must be 0 or greater");
    }

    @Test
    void validateRejectsTemplateCacheCheckWhenMissingFields() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("Node 1");
        config.setNodeVersion("1.0.0");
        config.setRegion("local");
        config.setCapacitySlots(4);
        config.setBrainBaseUrl("http://brain:8080");
        config.setCacheDir("./cache");
        config.getTemplateCacheCheck().setEnabled(true);

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("node-agent.template-cache-check.template-id is required")
                .hasMessageContaining("node-agent.template-cache-check.version is required")
                .hasMessageContaining("node-agent.template-cache-check.checksum is required");
    }

    @Test
    void validateRejectsNegativeVariableSubstitutionLimit() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("Node 1");
        config.setNodeVersion("1.0.0");
        config.setRegion("local");
        config.setCapacitySlots(4);
        config.setBrainBaseUrl("http://brain:8080");
        config.setCacheDir("./cache");
        config.getVariableSubstitution().setMaxFileBytes(-1);

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("node-agent.variable-substitution.max-file-bytes must be 0 or greater");
    }

    @Test
    void validateAcceptsTemplateCacheCheckWhenConfigured() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("Node 1");
        config.setNodeVersion("1.0.0");
        config.setRegion("local");
        config.setCapacitySlots(4);
        config.setBrainBaseUrl("http://brain:8080");
        config.setCacheDir("./cache");
        config.getTemplateCacheCheck().setEnabled(true);
        config.getTemplateCacheCheck().setTemplateId("starter");
        config.getTemplateCacheCheck().setVersion("1.2.3");
        config.getTemplateCacheCheck().setChecksum("abc123");

        assertThatNoException().isThrownBy(config::validate);
    }
}
