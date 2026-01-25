package net.spookly.kodama.nodeagent.registration;

import static org.assertj.core.api.Assertions.assertThat;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;

class NodeRegistrationRequestTest {

    @Test
    void buildsRequestFromNodeConfig() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("node-1");
        config.setNodeVersion("1.2.3");
        config.setRegion("eu-west");
        config.setCapacitySlots(12);
        config.setDevMode(true);
        config.setTags("gpu,ssd");
        config.setBaseUrl("http://node-1:9000");

        NodeRegistrationRequest request = NodeRegistrationRequest.fromConfig(config);

        assertThat(request.getName()).isEqualTo("node-1");
        assertThat(request.getNodeVersion()).isEqualTo("1.2.3");
        assertThat(request.getRegion()).isEqualTo("eu-west");
        assertThat(request.getCapacitySlots()).isEqualTo(12);
        assertThat(request.isDevMode()).isTrue();
        assertThat(request.getTags()).isEqualTo("gpu,ssd");
        assertThat(request.getBaseUrl()).isEqualTo("http://node-1:9000");
    }
}
