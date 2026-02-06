package net.spookly.kodama.nodeagent.instance.registry;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;

public record InstanceRegistryEntry(
        UUID instanceId,
        String name,
        String displayName,
        String portsJson,
        Map<String, String> variables,
        List<NodePrepareInstanceLayer> layers,
        OffsetDateTime preparedAt,
        String containerId,
        String containerStatus,
        OffsetDateTime containerStatusUpdatedAt,
        Integer containerExitCode,
        String containerExitReason,
        String workspacePath
) {
}
