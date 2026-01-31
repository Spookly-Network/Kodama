package net.spookly.kodama.nodeagent.instance.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import org.springframework.stereotype.Component;

@Component
public class InstancePortBindingsResolver {

    private static final TypeReference<Map<String, Object>> PORTS_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public InstancePortBindingsResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<DockerPortBinding> resolveBindings(InstanceRegistryEntry registry) {
        if (registry == null) {
            throw new InstanceStartException("Instance registry is required to resolve ports");
        }
        Map<String, Integer> containerPorts = parseContainerPorts(registry.portsJson());
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

    private Map<String, Integer> parseContainerPorts(String portsJson) {
        if (portsJson == null || portsJson.isBlank()) {
            return Map.of();
        }
        Map<String, Object> raw;
        try {
            raw = objectMapper.readValue(portsJson, PORTS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new InstanceStartException("portsJson must be a JSON object with numeric values", ex);
        }
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> ports = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String name = entry.getKey();
            int port = parsePort(entry.getValue(), "portsJson", name);
            ports.put(name, port);
        }
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
        if (value instanceof Number number) {
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
}
