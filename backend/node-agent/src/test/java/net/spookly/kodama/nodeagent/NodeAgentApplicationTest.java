package net.spookly.kodama.nodeagent;

import static org.assertj.core.api.Assertions.assertThat;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "node-agent.node-name=test-node",
        "node-agent.node-version=1.0.0",
        "node-agent.region=local",
        "node-agent.capacity-slots=4",
        "node-agent.brain-base-url=http://localhost:8080",
        "node-agent.cache-dir=./cache",
        "node-agent.registration-enabled=false",
        "node-agent.s3.region=local",
        "node-agent.s3.bucket=templates",
        "node-agent.s3.access-key=test-access",
        "node-agent.s3.secret-key=test-secret",
        "node-agent.s3.endpoint=http://localhost:9000"
})
class NodeAgentApplicationTest {

    @Autowired
    private NodeConfig config;

    @Test
    void loadsConfiguration() {
        assertThat(config.getNodeName()).isEqualTo("test-node");
        assertThat(config.getNodeVersion()).isEqualTo("1.0.0");
        assertThat(config.getRegion()).isEqualTo("local");
        assertThat(config.getCapacitySlots()).isEqualTo(4);
        assertThat(config.getBrainBaseUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.getCacheDir()).isEqualTo("./cache");
        assertThat(config.getWorkspaceDir()).isEqualTo("./data");
        assertThat(config.isRegistrationEnabled()).isFalse();
        assertThat(config.getS3().getRegion()).isEqualTo("local");
        assertThat(config.getS3().getBucket()).isEqualTo("templates");
        assertThat(config.getS3().getAccessKey()).isEqualTo("test-access");
        assertThat(config.getS3().getSecretKey()).isEqualTo("test-secret");
        assertThat(config.getS3().getEndpoint()).isEqualTo("http://localhost:9000");
    }
}
