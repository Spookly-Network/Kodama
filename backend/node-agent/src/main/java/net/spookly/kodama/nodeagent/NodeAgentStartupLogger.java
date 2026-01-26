package net.spookly.kodama.nodeagent;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NodeAgentStartupLogger implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(NodeAgentStartupLogger.class);

    private final NodeConfig config;

    public NodeAgentStartupLogger(NodeConfig config) {
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info(
                "Node agent started. nodeId={}, nodeName={}, nodeVersion={}, region={}, capacitySlots={}, " +
                        "brainBaseUrl={}, registrationEnabled={}, heartbeatIntervalSeconds={}, dockerHost={}",
                valueOrDash(config.getNodeId()),
                config.getNodeName(),
                config.getNodeVersion(),
                config.getRegion(),
                config.getCapacitySlots(),
                config.getBrainBaseUrl(),
                config.isRegistrationEnabled(),
                config.getHeartbeatIntervalSeconds(),
                valueOrDash(config.getDockerHost())
        );
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
