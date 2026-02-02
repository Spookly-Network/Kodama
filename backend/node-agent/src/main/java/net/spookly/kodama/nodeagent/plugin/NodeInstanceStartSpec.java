package net.spookly.kodama.nodeagent.plugin;

import java.util.List;
import java.util.Map;

public record NodeInstanceStartSpec(
        Map<String, String> env,
        Map<String, String> labels,
        List<String> command
) {
    public NodeInstanceStartSpec {
        env = env == null ? Map.of() : Map.copyOf(env);
        labels = labels == null ? Map.of() : Map.copyOf(labels);
        command = command == null ? null : List.copyOf(command);
    }
}
