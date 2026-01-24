package net.spookly.kodama.nodeagent;

import net.spookly.kodama.nodeagent.config.NodeAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NodeAgentProperties.class)
public class NodeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NodeAgentApplication.class, args);
    }
}
