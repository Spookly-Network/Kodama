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
        if (!portsJsonResult.directBindings().isEmpty()) {
            return portsJsonResult.directBindings();
        }
        Map<String, Integer> containerPorts = portsJsonResult.containerPorts();
        Map<String, Integer> hostPorts = parseHostPorts(registry.variables());
        if (!containerPorts.isEmpty()) {
            return resolveFromContainerPorts(containerPorts, hostPorts);
        }
        return resolveFromHostPorts(hostPorts);
    }

    private List<DockerPortBinding> resolveFromContainerPorts(
            Map<String, Integer> containerPorts,
            Map<String, Integer> hostPorts
    ) {
        List<DockerPortBinding> bindings = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : containerPorts.entrySet()) {
            String portName = entry.getKey();
            int containerPort = entry.getValue();
            String normalizedName = normalizePortName(portName);
            String hostKey = "PORT_" + normalizedName;
            Integer hostPort = hostPorts.get(hostKey);
            if (hostPort == null && containerPorts.size() == 1) {
                hostPort = hostPorts.get("PORT");
            }
            if (hostPort == null) {
                throw new InstanceStartException("Missing host port mapping for " + portName);
            }
            bindings.add(new DockerPortBinding(containerPort, hostPort, null));
        }
        return bindings;
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
            return PortsJsonResult.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(portsJson);
            if (root == null || root.isNull()) {
                return PortsJsonResult.empty();
            }
            if (root.isArray()) {
                return new PortsJsonResult(parseDirectBindings(root), Map.of());
            }
            if (root.isObject()) {
                return new PortsJsonResult(List.of(), parseLegacyContainerPorts(root));
            }
            throw new InstanceStartException("portsJson must be a JSON object or array");
        } catch (JsonProcessingException ex) {
            throw new InstanceStartException("portsJson must be a JSON object or array", ex);
        }
    }

    private List<DockerPortBinding> parseDirectBindings(JsonNode root) {
        List<DockerPortBinding> bindings = new ArrayList<>();
        int index = 0;
        for (JsonNode entry : root) {
            if (entry == null || !entry.isObject()) {
                throw new InstanceStartException("portsJson array entry must be an object at index " + index);
            }
            int containerPort = parsePort(entry.get("containerPort"), "portsJson[" + index + "].containerPort");
            int hostPort = parsePort(entry.get("hostPort"), "portsJson[" + index + "].hostPort");
            String protocol = parseProtocol(entry.get("protocol"), index);
            bindings.add(new DockerPortBinding(containerPort, hostPort, protocol));
            index++;
        }
        return bindings;
    }

    private Map<String, Integer> parseLegacyContainerPorts(JsonNode root) {
        Map<String, Integer> ports = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            int port = parsePort(entry.getValue(), "portsJson", name);
            ports.put(name, port);
        });
        return ports;
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

    private String normalizePortName(String name) {
        if (name == null || name.isBlank()) {
            return "PORT";
        }
        StringBuilder normalized = new StringBuilder();
        for (char ch : name.trim().toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                normalized.append(Character.toUpperCase(ch));
            } else {
                normalized.append('_');
            }
        }
        String value = normalized.toString();
        if (value.isEmpty()) {
            return "PORT";
        }
        return value;
    }

    private record PortsJsonResult(
            List<DockerPortBinding> directBindings,
            Map<String, Integer> containerPorts
    ) {
        private static PortsJsonResult empty() {
            return new PortsJsonResult(List.of(), Map.of());
        }
    }
}
