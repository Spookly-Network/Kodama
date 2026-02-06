package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceManager;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceDestroyServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void destroyStopsRemovesContainerAndWorkspace() throws Exception {
        UUID instanceId = UUID.randomUUID();
        String containerId = "container-1";
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.toString());
        InstanceProperties instanceProperties = new InstanceProperties();

        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);
        InstanceWorkspacePaths workspace = workspaceManager.prepareWorkspace(instanceId.toString());

        ObjectMapper objectMapper = objectMapper();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                instanceId,
                "instance-name",
                null,
                null,
                Map.of(),
                List.of(),
                OffsetDateTime.now(),
                containerId,
                "running",
                OffsetDateTime.now(),
                null,
                null,
                workspace.instanceRoot().toAbsolutePath().normalize().toString()
        );
        objectMapper.writeValue(workspace.instanceRoot().resolve("instance.json").toFile(), entry);

        DockerService dockerService = mock(DockerService.class);
        when(dockerService.inspectContainerIfExists(containerId))
                .thenReturn(status(containerId, true, "running"), status(containerId, false, "exited"));

        InstanceDestroyService service = new InstanceDestroyService(
                dockerService,
                registryService,
                layout,
                workspaceManager,
                config,
                instanceProperties
        );

        service.destroyInstance(instanceId);

        verify(dockerService).stopContainer(containerId, null);
        verify(dockerService).removeContainer(containerId, true, false);
        assertThat(Files.exists(workspace.instanceRoot())).isFalse();
    }

    @Test
    void destroySkipsMissingWorkspaceAndContainer() {
        UUID instanceId = UUID.randomUUID();
        String containerName = "kodama-instance-" + instanceId;
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.toString());
        InstanceProperties instanceProperties = new InstanceProperties();

        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);

        DockerService dockerService = mock(DockerService.class);
        when(dockerService.inspectContainerIfExists(containerName)).thenReturn(null);

        InstanceDestroyService service = new InstanceDestroyService(
                dockerService,
                registryService,
                layout,
                workspaceManager,
                config,
                instanceProperties
        );

        assertThatNoException().isThrownBy(() -> service.destroyInstance(instanceId));

        verify(dockerService, never()).stopContainer(anyString(), any());
        verify(dockerService, never()).removeContainer(anyString(), anyBoolean(), anyBoolean());
    }

    private DockerContainerStatus status(String containerId, Boolean running, String state) {
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
                null,
                null,
                null,
                null
        );
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
