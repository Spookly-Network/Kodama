package net.spookly.kodama.nodeagent;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NodeAgentStartupLogger implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(NodeAgentStartupLogger.class);

    private final NodeConfig config;

    public NodeAgentStartupLogger(NodeConfig config) {
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) {
        config.validate();
        logger.info(
                "Node agent started with config. nodeId={}, nodeName={}, brainBaseUrl={}, dockerHost={}, workspaceDir={}, cacheDir={}, authTokenPath={}, authCertPath={}, s3Endpoint={}, s3Region={}, s3Bucket={}, s3AccessKey={}, s3SecretKey={}",
                config.getNodeId(),
                config.getNodeName(),
                config.getBrainBaseUrl(),
                valueOrDash(config.getDockerHost()),
                config.getWorkspaceDir(),
                config.getCacheDir(),
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
