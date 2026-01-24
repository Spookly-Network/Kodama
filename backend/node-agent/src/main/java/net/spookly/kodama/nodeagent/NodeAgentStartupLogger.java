package net.spookly.kodama.nodeagent;

import net.spookly.kodama.nodeagent.config.NodeAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NodeAgentStartupLogger implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(NodeAgentStartupLogger.class);

    private final NodeAgentProperties properties;

    public NodeAgentStartupLogger(NodeAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info(
                "Node agent started. nodeId={}, nodeName={}, workspaceDir={}, brainBaseUrl={}, dockerHost={}",
                properties.getNodeId(),
                properties.getNodeName(),
                properties.getWorkspaceDir(),
                valueOrDash(properties.getBrainBaseUrl()),
                valueOrDash(properties.getDockerHost())
        );
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
