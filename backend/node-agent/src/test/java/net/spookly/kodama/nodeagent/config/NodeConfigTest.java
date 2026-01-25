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
}
