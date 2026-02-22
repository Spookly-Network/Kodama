package net.spookly.kodama.brain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.PortDefinitionRequest;
import net.spookly.kodama.brain.dto.node.NodeInstanceCommandRequest;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceLayer;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceRequest;
import net.spookly.kodama.brain.plugin.BrainPluginRegistry;
import net.spookly.kodama.brain.plugin.BrainPrepareInstanceContext;
import net.spookly.kodama.brain.plugin.BrainPrepareInstanceLayer;
import net.spookly.kodama.brain.plugin.BrainPrepareInstanceVariables;
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
  private static final TypeReference<List<String>> START_COMMAND_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<PortDefinitionRequest>> PORT_DEFINITIONS_TYPE =
      new TypeReference<>() {};

  private final RestTemplate restTemplate;
  private final NodeProperties nodeProperties;
  private final BrainPluginRegistry pluginRegistry;
  private final BrainSecurityProperties securityProperties;
  private final ObjectMapper objectMapper;

  public CommandDispatcherService(
      RestTemplate restTemplate,
      NodeProperties nodeProperties,
      BrainPluginRegistry pluginRegistry,
      BrainSecurityProperties brainSecurityProperties,
      ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.nodeProperties = nodeProperties;
    this.securityProperties = brainSecurityProperties;
    this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public void sendPrepareInstance(
      Node node,
      Instance instance,
      List<ResolvedTemplateLayer> layers,
      Map<String, String> variables) {
    Objects.requireNonNull(layers, "layers");
    UUID instanceId = requireInstanceId(instance);
    List<NodePrepareInstanceLayer> payloadLayers =
        layers.stream().map(this::toPrepareLayer).toList();
    String variablesJson = variables == null ? instance.getVariablesJson() : null;
    BrainPrepareInstanceContext context =
        new BrainPrepareInstanceContext(
            instanceId,
            instance.getName(),
            instance.getDisplayName(),
            instance.getPortsJson(),
            variables,
            variablesJson,
            layers.stream().map(this::toPluginLayer).toList());
    BrainPrepareInstanceVariables resolved =
        pluginRegistry.resolvePrepareInstanceVariables(context, variables, variablesJson);
    NodePrepareInstanceRequest payload =
        new NodePrepareInstanceRequest(
            instanceId,
            instance.getName(),
            instance.getDisplayName(),
            instance.getContainerImage(),
            instance.getInstallScript(),
            resolveStartCommand(instance),
            instance.getSlotsRequired(),
            resolvePortDefinitions(instance),
            instance.getPortsJson(),
            resolved.variables(),
            resolved.variablesJson(),
            payloadLayers);
    sendCommand(
        node,
        instance,
        "prepare",
        HttpMethod.POST,
        buildCommandUri(node, instanceId, "prepare"),
        payload);
  }

  public void sendStartInstance(Node node, Instance instance) {
    sendInstanceCommand(node, instance, "start");
  }

  public void sendStopInstance(Node node, Instance instance) {
    sendInstanceCommand(node, instance, "stop");
  }

  public void sendDestroyInstance(Node node, Instance instance) {
    sendInstanceCommand(node, instance, "destroy");
  }

  private void sendInstanceCommand(Node node, Instance instance, String action) {
    UUID instanceId = requireInstanceId(instance);
    NodeInstanceCommandRequest payload =
        new NodeInstanceCommandRequest(instanceId, instance.getName());
    sendCommand(
        node,
        instance,
        action,
        HttpMethod.POST,
        buildCommandUri(node, instanceId, action),
        payload);
  }

  private void sendCommand(
      Node node, Instance instance, String action, HttpMethod method, URI uri, Object payload) {
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
            ex);
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

    // TODO: Replace basic token auth
    headers.set(
        securityProperties.getNode().getHeaderName(), securityProperties.getNode().getToken());
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
    nodeProperties.requireHttpsBaseUrl(baseUrl, "Node baseUrl");
    return UriComponentsBuilder.fromUriString(baseUrl)
        .path("/api/instances/")
        .path(instanceId.toString())
        .path("/")
        .path(action)
        .build()
        .toUri();
  }

  private NodePrepareInstanceLayer toPrepareLayer(ResolvedTemplateLayer layer) {
    TemplateVersion version = layer.templateVersion();
    return new NodePrepareInstanceLayer(
        version.getId(),
        version.getTemplate().getId(),
        version.getVersion(),
        version.getChecksum(),
        version.getS3Key(),
        version.getMetadataJson(),
        layer.orderIndex());
  }

  private BrainPrepareInstanceLayer toPluginLayer(ResolvedTemplateLayer layer) {
    TemplateVersion version = layer.templateVersion();
    return new BrainPrepareInstanceLayer(
        version.getId(),
        version.getTemplate().getId(),
        version.getVersion(),
        version.getChecksum(),
        version.getS3Key(),
        version.getMetadataJson(),
        layer.orderIndex());
  }

  private UUID requireInstanceId(Instance instance) {
    UUID id = Objects.requireNonNull(instance, "instance").getId();
    if (id == null) {
      throw new IllegalStateException("Instance id is required to dispatch node commands");
    }
    return id;
  }

  private List<String> resolveStartCommand(Instance instance) {
    String startCommandJson = instance.getStartCommandJson();
    if (startCommandJson == null) {
      return null;
    }
    return deserializeJson(
        startCommandJson, START_COMMAND_TYPE, "Instance startCommandJson is invalid JSON");
  }

  private List<PortDefinitionRequest> resolvePortDefinitions(Instance instance) {
    String portDefinitionsJson = instance.getPortDefinitionsJson();
    if (portDefinitionsJson == null) {
      return null;
    }
    return deserializeJson(
        portDefinitionsJson, PORT_DEFINITIONS_TYPE, "Instance portDefinitionsJson is invalid JSON");
  }

  private <T> T deserializeJson(String json, TypeReference<T> type, String errorMessage) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(errorMessage, ex);
    }
  }
}
