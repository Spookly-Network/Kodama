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
                "Node agent started with config. nodeId={}, nodeName={}, nodeVersion={}, region={}, capacitySlots={}, " +
                        "devMode={}, tags={}, baseUrl={}, brainBaseUrl={}, registrationEnabled={}, dockerHost={}, " +
                        "workspaceDir={}, cacheDir={}, authHeaderName={}, authTokenPath={}, authCertPath={}, " +
                        "s3Endpoint={}, s3Region={}, s3Bucket={}, s3AccessKey={}, s3SecretKey={}",
                valueOrDash(config.getNodeId()),
                config.getNodeName(),
                config.getNodeVersion(),
                config.getRegion(),
                config.getCapacitySlots(),
                config.isDevMode(),
                valueOrDash(config.getTags()),
                valueOrDash(config.getBaseUrl()),
                config.getBrainBaseUrl(),
                config.isRegistrationEnabled(),
                valueOrDash(config.getDockerHost()),
                config.getWorkspaceDir(),
                config.getCacheDir(),
                valueOrDash(config.getAuth().getHeaderName()),
                valueOrDash(config.getAuth().getTokenPath()),
                valueOrDash(config.getAuth().getCertPath()),
                valueOrDash(config.getS3().getEndpoint()),
                valueOrDash(config.getS3().getRegion()),
                valueOrDash(config.getS3().getBucket()),
                redactIfPresent(config.getS3().getAccessKey()),
                redactIfPresent(config.getS3().getSecretKey())
        );
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String redactIfPresent(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return "<redacted>";
    }
}
