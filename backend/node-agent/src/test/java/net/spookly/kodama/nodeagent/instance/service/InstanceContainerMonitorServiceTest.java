package net.spookly.kodama.nodeagent.instance.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;

class InstanceContainerMonitorServiceTest {

    @Test
    void monitorRecordsExitCodeWhenContainerStops() {
        UUID instanceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OffsetDateTime createdAt = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2025-01-01T00:05:00Z");
        String containerId = "container-1";
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                instanceId,
                "instance-name",
                null,
                null,
                Map.of(),
                List.of(),
                createdAt,
                containerId,
                "running",
                updatedAt,
                null,
                null,
                "instances/" + instanceId
        );
        InstanceWorkspacePaths workspace = new InstanceWorkspacePaths(
                instanceId.toString(),
                Path.of("instances", instanceId.toString()),
                Path.of("instances", instanceId.toString(), "merged"),
                Path.of("instances", instanceId.toString(), "logs"),
                Path.of("instances", instanceId.toString(), "temp")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

        when(registryService.listRegistries()).thenReturn(List.of(entry));
        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(dockerService.inspectContainerIfExists(containerId))
                .thenReturn(status(containerId, false, "exited", 137, "crashed"));

        InstanceContainerMonitorService service = new InstanceContainerMonitorService(
                dockerService,
                registryService,
                workspaceLayout
        );

        service.monitorOnce();

        verify(registryService).recordContainerStatus(workspace, instanceId, "stopped", 137, "crashed");
    }

    private DockerContainerStatus status(
            String containerId,
            Boolean running,
            String state,
            Integer exitCode,
            String error
    ) {
        return new DockerContainerStatus(
                containerId,
                null,
                null,
                state,
                running,
                null,
                null,
                null,
                null,
                exitCode,
                error,
                null,
                null
        );
    }
}
