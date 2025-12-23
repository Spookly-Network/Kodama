package net.spookly.kodama.brain.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.node.NodeInstanceCommandRequest;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceLayer;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CommandDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcherService.class);

    private final RestTemplate restTemplate;
    private final NodeProperties nodeProperties;

    public CommandDispatcherService(RestTemplate restTemplate, NodeProperties nodeProperties) {
        this.restTemplate = restTemplate;
        this.nodeProperties = nodeProperties;
    }

    public void sendPrepareInstance(
            Node node,
            Instance instance,
            List<InstanceTemplateLayer> layers,
            Map<String, String> variables
    ) {
        Objects.requireNonNull(layers, "layers");
        UUID instanceId = requireInstanceId(instance);
        List<NodePrepareInstanceLayer> payloadLayers = layers.stream()
                .map(this::toPrepareLayer)
                .toList();
        NodePrepareInstanceRequest payload = new NodePrepareInstanceRequest(
                instanceId,
                instance.getName(),
                instance.getDisplayName(),
                instance.getPortsJson(),
                variables,
                variables == null ? instance.getVariablesJson() : null,
                payloadLayers
        );
        sendCommand(node, instance, "prepare", HttpMethod.POST, buildCommandUri(node, instanceId, "prepare"), payload);
    }

    public void sendStartInstance(Node node, Instance instance) {
        UUID instanceId = requireInstanceId(instance);
        NodeInstanceCommandRequest payload = new NodeInstanceCommandRequest(instanceId, instance.getName());
        sendCommand(node, instance, "start", HttpMethod.POST, buildCommandUri(node, instanceId, "start"), payload);
    }

    public void sendStopInstance(Node node, Instance instance) {
        UUID instanceId = requireInstanceId(instance);
        NodeInstanceCommandRequest payload = new NodeInstanceCommandRequest(instanceId, instance.getName());
        sendCommand(node, instance, "stop", HttpMethod.POST, buildCommandUri(node, instanceId, "stop"), payload);
    }

    public void sendDestroyInstance(Node node, Instance instance) {
        UUID instanceId = requireInstanceId(instance);
        NodeInstanceCommandRequest payload = new NodeInstanceCommandRequest(instanceId, instance.getName());
        sendCommand(node, instance, "destroy", HttpMethod.POST, buildCommandUri(node, instanceId, "destroy"), payload);
    }

    private void sendCommand(
            Node node,
            Instance instance,
            String action,
            HttpMethod method,
            URI uri,
            Object payload
    ) {
        int maxAttempts = Math.max(1, nodeProperties.getCommandMaxAttempts());
        long backoffMillis = Math.max(0, nodeProperties.getCommandRetryBackoffMillis());
        HttpEntity<?> request = createRequest(payload);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restTemplate.exchange(uri, method, request, Void.class);
                return;
            } catch (Exception ex) {
                logger.warn(
                        "Node command failed action={} nodeId={} instanceId={} attempt={}/{} uri={}",
                        action,
                        node.getId(),
                        instance.getId(),
                        attempt,
                        maxAttempts,
                        uri,
                        ex
                );
                if (attempt >= maxAttempts || !shouldRetry(ex)) {
                    throw ex;
                }
                sleepBackoff(backoffMillis);
            }
        }
    }

    private HttpEntity<?> createRequest(Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return payload == null ? new HttpEntity<>(headers) : new HttpEntity<>(payload, headers);
    }

    private boolean shouldRetry(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        if (ex instanceof HttpStatusCodeException statusException) {
            return statusException.getStatusCode().is5xxServerError();
        }
        return false;
    }

    private void sleepBackoff(long backoffMillis) {
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying node command", ex);
        }
    }

    private URI buildCommandUri(Node node, UUID instanceId, String action) {
        String baseUrl = Objects.requireNonNull(node, "node").getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Node baseUrl is not configured for node " + node.getId());
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/instances/")
                .path(instanceId.toString())
                .path("/")
                .path(action)
                .build()
                .toUri();
    }

    private NodePrepareInstanceLayer toPrepareLayer(InstanceTemplateLayer layer) {
        TemplateVersion version = layer.getTemplateVersion();
        return new NodePrepareInstanceLayer(
                version.getId(),
                version.getVersion(),
                version.getChecksum(),
                version.getS3Key(),
                version.getMetadataJson(),
                layer.getOrderIndex()
        );
    }

    private UUID requireInstanceId(Instance instance) {
        UUID id = Objects.requireNonNull(instance, "instance").getId();
        if (id == null) {
            throw new IllegalStateException("Instance id is required to dispatch node commands");
        }
        return id;
    }
}
