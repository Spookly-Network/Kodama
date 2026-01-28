package net.spookly.kodama.nodeagent.instance.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NodePrepareInstanceRequest(
        UUID instanceId,
        String name,
        String displayName,
        String portsJson,
        Map<String, String> variables,
        String variablesJson,
        List<NodePrepareInstanceLayer> layers
) {
}
