package net.spookly.kodama.nodeagent.instance.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import org.springframework.stereotype.Component;

@Component
public class InstancePortBindingsResolver {

    private final ObjectMapper objectMapper;

    public InstancePortBindingsResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<DockerPortBinding> resolveBindings(InstanceRegistryEntry registry) {
        if (registry == null) {
            throw new InstanceStartException("Instance registry is required to resolve ports");
        }
        PortsJsonResult portsJsonResult = parsePortsJson(registry.portsJson());
        if (portsJsonResult.portsJsonPresent()) {
            return portsJsonResult.directBindings();
        }
        Map<String, Integer> hostPorts = parseHostPorts(registry.variables());
        return resolveFromHostPorts(hostPorts);
    }

    private List<DockerPortBinding> resolveFromHostPorts(Map<String, Integer> hostPorts) {
        if (hostPorts.isEmpty()) {
            return List.of();
        }
        List<DockerPortBinding> bindings = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : hostPorts.entrySet()) {
            int port = entry.getValue();
            bindings.add(new DockerPortBinding(port, port, null));
        }
        return bindings;
    }

    private PortsJsonResult parsePortsJson(String portsJson) {
        if (portsJson == null || portsJson.isBlank()) {
            return PortsJsonResult.empty(false);
        }
        try {
            JsonNode root = objectMapper.readTree(portsJson);
            if (root != null && root.isArray()) {
                return new PortsJsonResult(parseDirectBindings(root), true);
            }
            throw new InstanceStartException("portsJson must be a JSON array");
        } catch (JsonProcessingException ex) {
            throw new InstanceStartException("portsJson must be a JSON array", ex);
        }
    }

    private List<DockerPortBinding> parseDirectBindings(JsonNode root) {
        List<DockerPortBinding> bindings = new ArrayList<>();
        int index = 0;
        for (JsonNode entry : root) {
            if (entry == null || !entry.isObject()) {
                throw new InstanceStartException("portsJson array entry must be an object at index " + index);
            }
            parseName(entry.get("name"), index);
            int containerPort = parsePort(entry.get("containerPort"), "portsJson[" + index + "].containerPort");
            int hostPort = parsePort(entry.get("hostPort"), "portsJson[" + index + "].hostPort");
            String protocol = parseProtocol(entry.get("protocol"), index);
            bindings.add(new DockerPortBinding(containerPort, hostPort, protocol));
            index++;
        }
        return bindings;
    }

    private Map<String, Integer> parseHostPorts(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> hostPorts = new LinkedHashMap<>();
        boolean hasNamedPorts = false;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String normalizedKey = key.trim().toUpperCase(Locale.ROOT);
            if (!normalizedKey.equals("PORT") && !normalizedKey.startsWith("PORT_")) {
                continue;
            }
            int port = parsePort(entry.getValue(), "variables", key);
            hostPorts.put(normalizedKey, port);
            if (normalizedKey.startsWith("PORT_")) {
                hasNamedPorts = true;
            }
        }
        if (hasNamedPorts) {
            hostPorts.remove("PORT");
        }
        return hostPorts;
    }

    private int parsePort(Object value, String source, String key) {
        if (value == null) {
            throw new InstanceStartException(source + " contains null port for " + key);
        }
        int port;
        if (value instanceof JsonNode node) {
            if (node.isNull()) {
                throw new InstanceStartException(source + " contains null port for " + key);
            }
            if (node.isNumber()) {
                port = node.intValue();
            } else if (node.isTextual()) {
                String textValue = node.textValue().trim();
                if (textValue.isEmpty()) {
                    throw new InstanceStartException(source + " contains blank port for " + key);
                }
                try {
                    port = Integer.parseInt(textValue);
                } catch (NumberFormatException ex) {
                    throw new InstanceStartException(source + " contains non-numeric port for " + key, ex);
                }
            } else {
                throw new InstanceStartException(source + " contains non-numeric port for " + key);
            }
        } else if (value instanceof Number number) {
            port = number.intValue();
        } else {
            String text = value.toString().trim();
            if (text.isEmpty()) {
                throw new InstanceStartException(source + " contains blank port for " + key);
            }
            try {
                port = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                throw new InstanceStartException(source + " contains non-numeric port for " + key, ex);
            }
        }
        if (port <= 0 || port > 65535) {
            throw new InstanceStartException(source + " contains invalid port " + port + " for " + key);
        }
        return port;
    }

    private int parsePort(JsonNode value, String source) {
        return parsePort(value, source, source);
    }

    private String parseProtocol(JsonNode node, int index) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new InstanceStartException("portsJson[" + index + "].protocol must be a string");
        }
        String protocol = node.textValue().trim().toLowerCase(Locale.ROOT);
        if (protocol.isEmpty()) {
            return null;
        }
        if (!protocol.equals("tcp") && !protocol.equals("udp")) {
            throw new InstanceStartException("portsJson[" + index + "].protocol must be tcp or udp");
        }
        return protocol;
    }

    private String parseName(JsonNode node, int index) {
        if (node == null || node.isNull()) {
            throw new InstanceStartException("portsJson[" + index + "].name is required");
        }
        if (!node.isTextual()) {
            throw new InstanceStartException("portsJson[" + index + "].name must be a string");
        }
        String name = node.textValue().trim();
        if (name.isEmpty()) {
            throw new InstanceStartException("portsJson[" + index + "].name must not be blank");
        }
        return name;
    }

    private record PortsJsonResult(
            List<DockerPortBinding> directBindings,
            boolean portsJsonPresent
    ) {
        private static PortsJsonResult empty(boolean portsJsonPresent) {
            return new PortsJsonResult(List.of(), portsJsonPresent);
        }
    }
}
