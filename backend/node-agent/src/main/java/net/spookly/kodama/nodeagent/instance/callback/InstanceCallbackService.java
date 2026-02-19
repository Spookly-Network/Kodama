package net.spookly.kodama.nodeagent.instance.callback;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareException;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceCallbackService.class);

    private final NodeConfig config;
    private final InstanceProperties instanceProperties;
    private final NodeRegistrationState registrationState;
    private final NodeAuthTokenReader tokenReader;
    private final BrainCallbackClient callbackClient;
    private final ObjectMapper objectMapper;

    public InstanceCallbackService(
            NodeConfig config,
            InstanceProperties instanceProperties,
            NodeRegistrationState registrationState,
            NodeAuthTokenReader tokenReader,
            BrainCallbackClient callbackClient,
            ObjectMapper objectMapper
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.instanceProperties = Objects.requireNonNull(instanceProperties, "instanceProperties");
        this.registrationState = Objects.requireNonNull(registrationState, "registrationState");
        this.tokenReader = Objects.requireNonNull(tokenReader, "tokenReader");
        this.callbackClient = Objects.requireNonNull(callbackClient, "callbackClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void sendPrepared(UUID instanceId) {
        sendPrepared(instanceId, null);
    }

    public void sendPrepared(UUID instanceId, String portsJson) {
        sendCallback(instanceId, "prepared", createPreparedPayload(portsJson));
    }

    public void sendRunning(UUID instanceId) {
        sendCallback(instanceId, "running", null);
    }

    public void sendStopped(UUID instanceId) {
        sendCallback(instanceId, "stopped", null);
    }

    public void sendDestroyed(UUID instanceId) {
        sendCallback(instanceId, "destroyed", null);
    }

    public void sendFailed(UUID instanceId) {
        sendCallback(instanceId, "failed", null);
    }

    private void sendCallback(UUID instanceId, String action, String requestBody) {
        UUID nodeId = resolveNodeId();
        URI endpoint = buildEndpoint(nodeId, instanceId, action);
        String authToken = tokenReader.readToken();
        String headerName = config.getAuth().getHeaderName();
        if (authToken != null && !authToken.isBlank() && (headerName == null || headerName.isBlank())) {
            throw new InstancePrepareException("node-agent.auth.header-name is required for instance callbacks");
        }
        if (authToken == null || authToken.isBlank()) {
            authToken = null;
        }
        int maxAttempts = resolveMaxAttempts();
        long backoffMillis = resolveBackoffMillis();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                callbackClient.sendCallback(endpoint, headerName, authToken, requestBody);
                if (attempt > 1) {
                    logger.info(
                            "Instance callback succeeded after retry attempt {} action={} instanceId={}",
                            attempt,
                            action,
                            instanceId
                    );
                }
                return;
            } catch (RuntimeException ex) {
                if (attempt >= maxAttempts) {
                    logger.warn(
                            "Instance callback failed after {} attempts action={} instanceId={} endpoint={}",
                            maxAttempts,
                            action,
                            instanceId,
                            endpoint,
                            ex
                    );
                    throw ex;
                }
                logger.warn(
                        "Instance callback attempt {} failed, retrying in {}ms action={} instanceId={} endpoint={}",
                        attempt,
                        backoffMillis,
                        action,
                        instanceId,
                        endpoint,
                        ex
                );
                if (!sleep(backoffMillis)) {
                    throw ex;
                }
                backoffMillis = nextBackoffMillis(backoffMillis);
            }
        }
    }

    private String createPreparedPayload(String portsJson) {
        if (portsJson == null || portsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new InstancePreparedCallbackRequest(portsJson));
        } catch (JsonProcessingException ex) {
            throw new InstancePrepareException("Failed to serialize prepared callback payload", ex);
        }
    }

    private int resolveMaxAttempts() {
        InstanceProperties.InstanceCallbacks callbacks = instanceProperties.getInstanceCallbacks();
        if (callbacks == null) {
            return 1;
        }
        return Math.max(1, callbacks.getMaxAttempts());
    }

    private long resolveBackoffMillis() {
        InstanceProperties.InstanceCallbacks callbacks = instanceProperties.getInstanceCallbacks();
        if (callbacks == null) {
            return 0L;
        }
        return Math.max(0L, callbacks.getRetryBackoffMillis());
    }

    private long nextBackoffMillis(long backoffMillis) {
        if (backoffMillis <= 0) {
            return 0L;
        }
        long next = backoffMillis * 2;
        if (next < 0) {
            return backoffMillis;
        }
        return next;
    }

    private boolean sleep(long backoffMillis) {
        if (backoffMillis <= 0) {
            return true;
        }
        try {
            Thread.sleep(backoffMillis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("Instance callback retry interrupted");
            return false;
        }
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

    private record InstancePreparedCallbackRequest(String portsJson) {
    }
}
