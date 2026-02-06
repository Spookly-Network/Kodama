package net.spookly.kodama.nodeagent.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;

public record NodeInstanceStartContext(
        UUID instanceId,
        String requestedName,
        InstanceRegistryEntry registry,
        Map<String, String> env,
        Map<String, String> labels,
        List<String> command
) {
    public NodeInstanceStartContext {
        env = copyWithoutNulls(env);
        labels = copyWithoutNulls(labels);
        command = command == null ? null : List.copyOf(command);
    }

    private static Map<String, String> copyWithoutNulls(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> filtered = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered.isEmpty() ? Map.of() : Map.copyOf(filtered);
    }
}
