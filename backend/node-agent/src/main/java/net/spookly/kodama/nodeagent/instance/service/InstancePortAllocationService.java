package net.spookly.kodama.nodeagent.instance.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.instance.dto.NodePreparePortDefinition;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import org.springframework.stereotype.Component;

@Component
public class InstancePortAllocationService {

    private final InstanceRegistryService registryService;
    private final ObjectMapper objectMapper;

    public InstancePortAllocationService(InstanceRegistryService registryService, ObjectMapper objectMapper) {
        this.registryService = Objects.requireNonNull(registryService, "registryService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public PortAllocationResult allocate(UUID instanceId, List<NodePreparePortDefinition> definitions) {
        if (instanceId == null) {
            throw new InstancePrepareValidationException("instanceId is required for port allocation");
        }
        if (definitions == null || definitions.isEmpty()) {
            return PortAllocationResult.none();
        }

        Set<Integer> reservedHostPorts = loadReservedHostPorts(instanceId);
        List<AllocatedPort> allocatedPorts = new ArrayList<>(definitions.size());
        Set<String> normalizedNames = new LinkedHashSet<>();

        for (NodePreparePortDefinition definition : definitions) {
            AllocationDefinition normalized = normalizeDefinition(definition);
            if (!normalizedNames.add(normalized.normalizedName())) {
                throw new InstancePrepareValidationException(
                        "Duplicate port definition name after normalization: " + normalized.name()
                );
            }
            int hostPort = allocateHostPort(normalized.hostRange(), reservedHostPorts, normalized.name());
            reservedHostPorts.add(hostPort);
            allocatedPorts.add(new AllocatedPort(
                    normalized.name(),
                    normalized.protocol(),
                    normalized.containerPort(),
                    hostPort
            ));
        }

        Map<String, String> injectedVariables = buildInjectedVariables(allocatedPorts);
        String portsJson = serializeAllocatedPorts(allocatedPorts);
        return new PortAllocationResult(portsJson, injectedVariables);
    }

    private Set<Integer> loadReservedHostPorts(UUID currentInstanceId) {
        List<InstanceRegistryEntry> entries = registryService.listRegistriesForAllocation();
        Set<Integer> reserved = new LinkedHashSet<>();
        for (InstanceRegistryEntry entry : entries) {
            if (entry == null || entry.instanceId() == null) {
                continue;
            }
            if (currentInstanceId.equals(entry.instanceId())) {
                continue;
            }
            reserved.addAll(parseReservedHostPorts(entry));
        }
        return reserved;
    }

    private Set<Integer> parseReservedHostPorts(InstanceRegistryEntry entry) {
        Set<Integer> reserved = new LinkedHashSet<>();
        reserved.addAll(parseHostPortsFromPortsJson(entry));
        reserved.addAll(parseHostPortsFromVariables(entry));
        return reserved;
    }

    private Set<Integer> parseHostPortsFromPortsJson(InstanceRegistryEntry entry) {
        String portsJson = entry.portsJson();
        if (portsJson == null || portsJson.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = objectMapper.readTree(portsJson);
            Set<Integer> ports = new LinkedHashSet<>();
            if (root.isArray()) {
                int index = 0;
                for (JsonNode item : root) {
                    JsonNode hostPortNode = item.get("hostPort");
                    if (hostPortNode == null || hostPortNode.isNull()) {
                        throw new InstancePrepareException("portsJson array entry is missing hostPort at index " + index);
                    }
                    ports.add(parsePort(hostPortNode, "portsJson[" + index + "].hostPort"));
                    index++;
                }
                return ports;
            }
            if (root.isObject()) {
                root.fields().forEachRemaining(field -> {
                    JsonNode value = field.getValue();
                    if (value != null && value.isObject() && value.has("hostPort")) {
                        ports.add(parsePort(value.get("hostPort"), "portsJson." + field.getKey() + ".hostPort"));
                    } else {
                        ports.add(parsePort(value, "portsJson." + field.getKey()));
                    }
                });
                return ports;
            }
            throw new InstancePrepareException("portsJson must be a JSON object or array when loading reservations");
        } catch (IOException ex) {
            throw new InstancePrepareException(
                    "Failed to parse portsJson from registry for instance " + entry.instanceId(),
                    ex
            );
        }
    }

    private Set<Integer> parseHostPortsFromVariables(InstanceRegistryEntry entry) {
        Map<String, String> variables = entry.variables();
        if (variables == null || variables.isEmpty()) {
            return Set.of();
        }
        Set<Integer> ports = new LinkedHashSet<>();
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            String key = variable.getKey();
            if (key == null) {
                continue;
            }
            String normalizedKey = key.trim().toUpperCase(Locale.ROOT);
            if (!normalizedKey.equals("PORT") && !normalizedKey.startsWith("PORT_")) {
                continue;
            }
            ports.add(parsePort(variable.getValue(), "variables." + key));
        }
        return ports;
    }

    private int allocateHostPort(
            NodePreparePortDefinition.HostRange hostRange,
            Set<Integer> reservedHostPorts,
            String definitionName
    ) {
        int min = requirePort(hostRange.min(), "hostRange.min for " + definitionName);
        int max = requirePort(hostRange.max(), "hostRange.max for " + definitionName);
        int step = requirePositive(hostRange.step(), "hostRange.step for " + definitionName);
        if (min > max) {
            throw new InstancePrepareValidationException("hostRange min must be <= max for " + definitionName);
        }

        long candidate = min;
        while (candidate <= max) {
            int hostPort = (int) candidate;
            if (!reservedHostPorts.contains(hostPort)) {
                return hostPort;
            }
            candidate += step;
        }

        throw new InstancePrepareException(
                "No available host port for " + definitionName + " in range " + min + "-" + max + " (step " + step + ")"
        );
    }

    private AllocationDefinition normalizeDefinition(NodePreparePortDefinition definition) {
        if (definition == null) {
            throw new InstancePrepareValidationException("port definition is required");
        }
        String name = requireText(definition.name(), "port definition name");
        String protocol = normalizeProtocol(definition.protocol());
        int containerPort = requirePort(definition.containerPort(), "containerPort for " + name);
        NodePreparePortDefinition.HostRange hostRange = definition.hostRange();
        if (hostRange == null) {
            throw new InstancePrepareValidationException("hostRange is required for " + name);
        }
        return new AllocationDefinition(name, normalizePortName(name), protocol, containerPort, hostRange);
    }

    private Map<String, String> buildInjectedVariables(List<AllocatedPort> allocatedPorts) {
        if (allocatedPorts.isEmpty()) {
            return Map.of();
        }
        Map<String, String> injected = new LinkedHashMap<>();
        injected.put("PORT", Integer.toString(allocatedPorts.getFirst().hostPort()));
        for (AllocatedPort port : allocatedPorts) {
            injected.put("PORT_" + normalizePortName(port.name()), Integer.toString(port.hostPort()));
        }
        return injected;
    }

    private String serializeAllocatedPorts(List<AllocatedPort> allocatedPorts) {
        try {
            return objectMapper.writeValueAsString(allocatedPorts);
        } catch (IOException ex) {
            throw new InstancePrepareException("Failed to serialize allocated ports", ex);
        }
    }

    private String normalizeProtocol(String protocol) {
        String value = requireText(protocol, "port protocol").toLowerCase(Locale.ROOT);
        if (!"tcp".equals(value) && !"udp".equals(value)) {
            throw new InstancePrepareValidationException("port protocol must be tcp or udp");
        }
        return value;
    }

    private int requirePort(Integer port, String label) {
        if (port == null) {
            throw new InstancePrepareValidationException(label + " is required");
        }
        if (port < 1 || port > 65535) {
            throw new InstancePrepareValidationException(label + " must be between 1 and 65535");
        }
        return port;
    }

    private int requirePositive(Integer value, String label) {
        if (value == null) {
            throw new InstancePrepareValidationException(label + " is required");
        }
        if (value < 1) {
            throw new InstancePrepareValidationException(label + " must be >= 1");
        }
        return value;
    }

    private int parsePort(Object value, String source) {
        if (value == null) {
            throw new InstancePrepareException(source + " must not be null");
        }
        if (value instanceof JsonNode jsonNode) {
            return parsePortFromJsonNode(jsonNode, source);
        }
        String text = value.toString();
        return parsePortFromString(text, source);
    }

    private int parsePortFromJsonNode(JsonNode node, String source) {
        if (node == null || node.isNull()) {
            throw new InstancePrepareException(source + " must not be null");
        }
        if (node.isNumber()) {
            int port = node.intValue();
            if (port < 1 || port > 65535) {
                throw new InstancePrepareException(source + " must be between 1 and 65535");
            }
            return port;
        }
        if (node.isTextual()) {
            return parsePortFromString(node.textValue(), source);
        }
        throw new InstancePrepareException(source + " must be numeric");
    }

    private int parsePortFromString(String text, String source) {
        if (text == null || text.isBlank()) {
            throw new InstancePrepareException(source + " must not be blank");
        }
        final int port;
        try {
            port = Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new InstancePrepareException(source + " must be numeric", ex);
        }
        if (port < 1 || port > 65535) {
            throw new InstancePrepareException(source + " must be between 1 and 65535");
        }
        return port;
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InstancePrepareValidationException(label + " is required");
        }
        return value.trim();
    }

    private String normalizePortName(String name) {
        String text = requireText(name, "port definition name");
        StringBuilder normalized = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                normalized.append(Character.toUpperCase(ch));
            } else {
                normalized.append('_');
            }
        }
        String result = normalized.toString();
        if (result.isBlank()) {
            throw new InstancePrepareValidationException("port definition name must contain letters or digits");
        }
        return result;
    }

    public record PortAllocationResult(String portsJson, Map<String, String> injectedVariables) {

        public PortAllocationResult {
            if (injectedVariables == null || injectedVariables.isEmpty()) {
                injectedVariables = Map.of();
            } else {
                injectedVariables = Collections.unmodifiableMap(new LinkedHashMap<>(injectedVariables));
            }
        }

        public static PortAllocationResult none() {
            return new PortAllocationResult(null, Map.of());
        }
    }

    public record AllocatedPort(
            String name,
            String protocol,
            Integer containerPort,
            Integer hostPort
    ) {
    }

    private record AllocationDefinition(
            String name,
            String normalizedName,
            String protocol,
            int containerPort,
            NodePreparePortDefinition.HostRange hostRange
    ) {
    }
}
