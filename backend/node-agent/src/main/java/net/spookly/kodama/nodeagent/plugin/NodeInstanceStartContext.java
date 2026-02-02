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
        env = env == null ? Map.of() : Map.copyOf(env);
        labels = labels == null ? Map.of() : Map.copyOf(labels);
        command = command == null ? List.of() : List.copyOf(command);
    }
}
