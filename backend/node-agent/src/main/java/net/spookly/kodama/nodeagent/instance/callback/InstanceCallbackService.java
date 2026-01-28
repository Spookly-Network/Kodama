package net.spookly.kodama.nodeagent.instance.callback;

import java.net.URI;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareException;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.springframework.stereotype.Component;

@Component
public class InstanceCallbackService {

    private final NodeConfig config;
    private final NodeRegistrationState registrationState;
    private final NodeAuthTokenReader tokenReader;
    private final NodeCallbackClient callbackClient;

    public InstanceCallbackService(
            NodeConfig config,
            NodeRegistrationState registrationState,
            NodeAuthTokenReader tokenReader,
            NodeCallbackClient callbackClient
    ) {
        this.config = config;
        this.registrationState = registrationState;
        this.tokenReader = tokenReader;
        this.callbackClient = callbackClient;
    }

    public void sendPrepared(UUID instanceId) {
        sendCallback(instanceId, "prepared");
    }

    public void sendFailed(UUID instanceId) {
        sendCallback(instanceId, "failed");
    }

    private void sendCallback(UUID instanceId, String action) {
        UUID nodeId = resolveNodeId();
        URI endpoint = buildEndpoint(nodeId, instanceId, action);
        String authToken = tokenReader.readToken();
        if (authToken == null || authToken.isBlank()) {
            throw new InstancePrepareException("Node auth token is required for instance callbacks");
        }
        String headerName = config.getAuth().getHeaderName();
        if (headerName == null || headerName.isBlank()) {
            throw new InstancePrepareException("node-agent.auth.header-name is required for instance callbacks");
        }
        callbackClient.sendCallback(endpoint, headerName, authToken);
    }

    private UUID resolveNodeId() {
        UUID nodeId = registrationState.getNodeId();
        if (nodeId != null) {
            return nodeId;
        }
        String configuredNodeId = config.getNodeId();
        if (configuredNodeId == null || configuredNodeId.isBlank()) {
            throw new InstancePrepareException("node-agent.node-id is required for instance callbacks");
        }
        try {
            return UUID.fromString(configuredNodeId);
        } catch (IllegalArgumentException ex) {
            throw new InstancePrepareException("node-agent.node-id is invalid", ex);
        }
    }

    private URI buildEndpoint(UUID nodeId, UUID instanceId, String action) {
        String brainBaseUrl = config.getBrainBaseUrl();
        if (brainBaseUrl == null || brainBaseUrl.isBlank()) {
            throw new InstancePrepareException("node-agent.brain-base-url is required for instance callbacks");
        }
        String trimmed = brainBaseUrl.endsWith("/") ? brainBaseUrl.substring(0, brainBaseUrl.length() - 1) : brainBaseUrl;
        try {
            return URI.create(trimmed + "/api/nodes/" + nodeId + "/instances/" + instanceId + "/" + action);
        } catch (IllegalArgumentException ex) {
            throw new InstancePrepareException("Invalid Brain base URL: " + brainBaseUrl, ex);
        }
    }
}
