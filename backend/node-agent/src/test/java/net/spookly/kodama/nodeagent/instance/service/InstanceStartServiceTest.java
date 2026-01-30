package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.docker.dto.DockerVolumeMount;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class InstanceStartServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void startInstanceCreatesContainerAndRecordsId() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = new InstanceWorkspacePaths(
                instanceId.toString(),
                instanceRoot,
                mergedDir,
                instanceRoot.resolve("logs"),
                instanceRoot.resolve("temp")
        );

        InstanceRegistryEntry registry = new InstanceRegistryEntry(
                instanceId,
                "instance-name",
                null,
                null,
                Map.of("ENV", "prod"),
                List.of(),
                OffsetDateTime.now(),
                null
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(portBindingsResolver.resolveBindings(registry))
                .thenReturn(List.of(new DockerPortBinding(25565, 30000, null)));
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));

        NodeConfig config = new NodeConfig();
        config.setNodeName("node-1");
        config.getInstanceRuntime().setImage("image:test");
        config.getInstanceRuntime().setWorkspaceMountPath("/workspace");

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                config
        );

        service.startInstance(instanceId, "request-name");

        ArgumentCaptor<DockerContainerCreateRequest> captor = ArgumentCaptor.forClass(DockerContainerCreateRequest.class);
        verify(dockerService).createContainer(captor.capture());
        DockerContainerCreateRequest request = captor.getValue();
        assertThat(request.image()).isEqualTo("image:test");
        assertThat(request.name()).isEqualTo("kodama-instance-" + instanceId);
        assertThat(request.workingDir()).isEqualTo("/workspace");
        assertThat(request.env()).contains("INSTANCE_ID=" + instanceId, "NODE_NAME=node-1", "ENV=prod");
        assertThat(request.volumeMounts()).hasSize(1);
        DockerVolumeMount mount = request.volumeMounts().get(0);
        assertThat(mount.hostPath()).isEqualTo(mergedDir.toString());
        assertThat(mount.containerPath()).isEqualTo("/workspace");
        assertThat(request.portBindings()).hasSize(1);

        var order = inOrder(dockerService, registryService);
        order.verify(dockerService).startContainer("container-1");
        order.verify(registryService).recordContainerId(workspace, instanceId, "container-1");
    }
}
