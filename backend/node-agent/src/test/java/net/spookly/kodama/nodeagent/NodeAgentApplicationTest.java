package net.spookly.kodama.nodeagent;

import static org.assertj.core.api.Assertions.assertThat;

import net.spookly.kodama.nodeagent.config.NodeAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NodeAgentApplicationTest {

    @Autowired
    private NodeAgentProperties properties;

    @Test
    void loadsDefaultConfiguration() {
        assertThat(properties.getNodeId()).isEqualTo("local");
        assertThat(properties.getNodeName()).isEqualTo("local-node");
        assertThat(properties.getWorkspaceDir()).isEqualTo("./data");
    }
}
