package net.spookly.kodama.nodeagent;

import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.config.NodePluginsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        NodeConfig.class,
        InstanceProperties.class,
        NodePluginsProperties.class
})
public class NodeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NodeAgentApplication.class, args);
    }
}
