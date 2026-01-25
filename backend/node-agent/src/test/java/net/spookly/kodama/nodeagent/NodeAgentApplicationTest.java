package net.spookly.kodama.nodeagent;

import static org.assertj.core.api.Assertions.assertThat;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "node-agent.node-id=test-node",
        "node-agent.node-name=test-node",
        "node-agent.brain-base-url=http://localhost:8080",
        "node-agent.cache-dir=./cache"
})
class NodeAgentApplicationTest {

    @Autowired
    private NodeConfig config;

    @Test
    void loadsConfiguration() {
        assertThat(config.getNodeId()).isEqualTo("test-node");
        assertThat(config.getNodeName()).isEqualTo("test-node");
        assertThat(config.getBrainBaseUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.getCacheDir()).isEqualTo("./cache");
        assertThat(config.getWorkspaceDir()).isEqualTo("./data");
    }
}
