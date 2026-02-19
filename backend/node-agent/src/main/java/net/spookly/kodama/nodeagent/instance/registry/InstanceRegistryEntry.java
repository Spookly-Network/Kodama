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
        String containerImage,
        String installScript,
        List<String> startCommand,
        Integer slotsRequired,
        String portsJson,
        boolean installCompleted,
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

    public InstanceRegistryEntry {
        startCommand = startCommand == null ? List.of() : startCommand;
        slotsRequired = slotsRequired == null ? 1 : slotsRequired;
    }

    public InstanceRegistryEntry(
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
        this(
                instanceId,
                name,
                displayName,
                null,
                null,
                List.of(),
                1,
                portsJson,
                false,
                variables,
                layers,
                preparedAt,
                containerId,
                containerStatus,
                containerStatusUpdatedAt,
                containerExitCode,
                containerExitReason,
                workspacePath
        );
    }
}
