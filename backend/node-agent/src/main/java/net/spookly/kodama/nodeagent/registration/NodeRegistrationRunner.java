package net.spookly.kodama.nodeagent.registration;

import java.net.URI;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NodeRegistrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistrationRunner.class);

    private final NodeConfig config;
    private final NodeRegistrationClient registrationClient;
    private final NodeAuthTokenReader tokenReader;
    private final NodeRegistrationState registrationState;

    public NodeRegistrationRunner(
            NodeConfig config,
            NodeRegistrationClient registrationClient,
            NodeAuthTokenReader tokenReader,
            NodeRegistrationState registrationState
    ) {
        this.config = config;
        this.registrationClient = registrationClient;
        this.tokenReader = tokenReader;
        this.registrationState = registrationState;
    }

    @Override
    public void run(ApplicationArguments args) {
        config.validate();
        if (!config.isRegistrationEnabled()) {
            logger.info("Node registration is disabled. Skipping Brain registration.");
            return;
        }
        try {
            NodeRegistrationRequest request = NodeRegistrationRequest.fromConfig(config);
            URI endpoint = buildRegistrationEndpoint(config.getBrainBaseUrl());
            String token = tokenReader.readToken();
            String headerName = config.getAuth().getHeaderName();
            if ((token != null && !token.isBlank()) && (headerName == null || headerName.isBlank())) {
                throw new NodeRegistrationException("Node auth header name is required when a token is provided");
            }
            NodeRegistrationResponse response = registrationClient.register(endpoint, headerName, token, request);
            registrationState.update(response);
            config.setNodeId(response.getNodeId().toString());
            logger.info(
                    "Node registered with Brain. nodeId={}, heartbeatIntervalSeconds={}",
                    response.getNodeId(),
                    response.getHeartbeatIntervalSeconds()
            );
        } catch (RuntimeException ex) {
            logger.error("Node registration failed. Shutting down.", ex);
            throw ex;
        }
    }

    private URI buildRegistrationEndpoint(String brainBaseUrl) {
        if (brainBaseUrl == null || brainBaseUrl.isBlank()) {
            throw new NodeRegistrationException("Brain base URL is required for registration");
        }
        String trimmed = brainBaseUrl.endsWith("/") ? brainBaseUrl.substring(0, brainBaseUrl.length() - 1) : brainBaseUrl;
        try {
            return URI.create(trimmed + "/api/nodes/register");
        } catch (IllegalArgumentException ex) {
            throw new NodeRegistrationException("Invalid Brain base URL: " + brainBaseUrl, ex);
        }
    }
}
