package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerOperationException;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceStopServiceTest {

  @TempDir Path tempDir;

  @Test
  void stopInstanceStopsContainerAndUpdatesRegistry() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-1";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
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
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId))
        .thenReturn(status(containerId, true, "running"), status(containerId, false, "exited"));

    NodeConfig config = new NodeConfig();
    InstanceProperties instanceProperties = new InstanceProperties();
    instanceProperties.getInstanceRuntime().setStopTimeoutSeconds(15);

    InstanceStopService service =
        new InstanceStopService(
            dockerService, registryService, workspaceLayout, config, instanceProperties);

    service.stopInstance(instanceId);

    var order = inOrder(registryService, dockerService);
    order
        .verify(registryService)
        .recordContainerStatus(workspace, instanceId, "stopping", null, null);
    order.verify(dockerService).stopContainer(containerId, 15);
    order
        .verify(registryService)
        .recordContainerStatus(eq(workspace), eq(instanceId), eq("stopped"), any(), any());
    verify(dockerService, never()).killContainer(containerId);
  }

  @Test
  void stopInstanceContinuesWhenStoppingStatusWriteFails() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-stopping-failure";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
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
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId))
        .thenReturn(status(containerId, true, "running"), status(containerId, false, "exited"));
    doThrow(new RuntimeException("registry write failed"))
        .when(registryService)
        .recordContainerStatus(workspace, instanceId, "stopping", null, null);

    NodeConfig config = new NodeConfig();
    InstanceProperties instanceProperties = new InstanceProperties();
    instanceProperties.getInstanceRuntime().setStopTimeoutSeconds(10);

    InstanceStopService service =
        new InstanceStopService(
            dockerService, registryService, workspaceLayout, config, instanceProperties);

    assertThatNoException().isThrownBy(() -> service.stopInstance(instanceId));

    verify(dockerService).stopContainer(containerId, 10);
    verify(registryService)
        .recordContainerStatus(eq(workspace), eq(instanceId), eq("stopped"), any(), any());
  }

  @Test
  void stopInstanceKillsContainerWhenStillRunningAfterStop() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-2";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
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
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId))
        .thenReturn(
            status(containerId, true, "running"),
            status(containerId, true, "running"),
            status(containerId, false, "exited"));

    NodeConfig config = new NodeConfig();
    InstanceProperties instanceProperties = new InstanceProperties();

    InstanceStopService service =
        new InstanceStopService(
            dockerService, registryService, workspaceLayout, config, instanceProperties);

    service.stopInstance(instanceId);

    verify(dockerService).stopContainer(containerId, null);
    verify(dockerService).killContainer(containerId);
    verify(registryService)
        .recordContainerStatus(eq(workspace), eq(instanceId), eq("stopped"), any(), any());
  }

  @Test
  void stopInstanceTreatsNotFoundDuringStopAsStopped() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-3";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
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
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId))
        .thenReturn(status(containerId, true, "running"));
    doThrow(
            new DockerOperationException(
                "Docker container not found: " + containerId, new NotFoundException("missing")))
        .when(dockerService)
        .stopContainer(containerId, null);

    InstanceStopService service =
        new InstanceStopService(
            dockerService,
            registryService,
            workspaceLayout,
            new NodeConfig(),
            new InstanceProperties());

    assertThatNoException().isThrownBy(() -> service.stopInstance(instanceId));

    verify(dockerService, never()).killContainer(containerId);
    verify(registryService)
        .recordContainerStatus(eq(workspace), eq(instanceId), eq("stopped"), any(), any());
  }

  @Test
  void stopInstanceTreatsNotRunningDuringStopAsStopped() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-4";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
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
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId))
        .thenReturn(status(containerId, true, "running"));
    doThrow(
            new DockerOperationException(
                "Docker container not running: " + containerId,
                new NotModifiedException("stopped")))
        .when(dockerService)
        .stopContainer(containerId, null);

    InstanceStopService service =
        new InstanceStopService(
            dockerService,
            registryService,
            workspaceLayout,
            new NodeConfig(),
            new InstanceProperties());

    assertThatNoException().isThrownBy(() -> service.stopInstance(instanceId));

    verify(dockerService, never()).killContainer(containerId);
    verify(registryService)
        .recordContainerStatus(eq(workspace), eq(instanceId), eq("stopped"), any(), any());
  }

  @Test
  void stopInstancePreservesExitMetadataWhenContainerMissing() throws Exception {
    UUID instanceId = UUID.randomUUID();
    String containerId = "container-5";
    InstanceWorkspacePaths workspace = createWorkspace(instanceId);
    InstanceRegistryEntry registry =
        new InstanceRegistryEntry(
            instanceId,
            "instance-name",
            null,
            null,
            Map.of(),
            List.of(),
            OffsetDateTime.now(),
            containerId,
            "stopped",
            OffsetDateTime.now(),
            137,
            "crashed",
            null);

    DockerService dockerService = mock(DockerService.class);
    InstanceRegistryService registryService = mock(InstanceRegistryService.class);
    InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);

    when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
    when(registryService.loadRegistry(workspace)).thenReturn(registry);
    when(dockerService.inspectContainerIfExists(containerId)).thenReturn(null);

    InstanceStopService service =
        new InstanceStopService(
            dockerService,
            registryService,
            workspaceLayout,
            new NodeConfig(),
            new InstanceProperties());

    assertThatNoException().isThrownBy(() -> service.stopInstance(instanceId));

    verify(registryService)
        .recordContainerStatus(
            eq(workspace), eq(instanceId), eq("stopped"), eq(137), eq("crashed"));
  }

  private InstanceWorkspacePaths createWorkspace(UUID instanceId) throws Exception {
    Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
    Files.createDirectories(instanceRoot);
    return new InstanceWorkspacePaths(
        instanceId.toString(),
        instanceRoot,
        instanceRoot.resolve("merged"),
        instanceRoot.resolve("logs"),
        instanceRoot.resolve("temp"));
  }

  private DockerContainerStatus status(String containerId, Boolean running, String state) {
    return new DockerContainerStatus(
        containerId, null, null, state, running, null, null, null, null, null, null, null, null);
  }
}
