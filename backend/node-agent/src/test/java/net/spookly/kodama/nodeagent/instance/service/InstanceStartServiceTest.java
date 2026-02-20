package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.config.NodePluginsProperties;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.docker.dto.DockerVolumeMount;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import net.spookly.kodama.nodeagent.plugin.NodePluginRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class InstanceStartServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void startInstanceRunsInstallScriptOnceAndUsesRegistryRuntimeFields() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry pendingInstall = registryEntry(
                instanceId,
                instanceRoot,
                false,
                "printf 'ok' > .install-complete",
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );
        InstanceRegistryEntry installComplete = registryEntry(
                instanceId,
                instanceRoot,
                true,
                pendingInstall.installScript(),
                pendingInstall.containerImage(),
                pendingInstall.startCommand()
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(pendingInstall, installComplete);
        when(portBindingsResolver.resolveBindings(installComplete))
                .thenReturn(List.of(new DockerPortBinding(25565, 30000, null)));
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));
        when(installScriptRunner.runScript(any(), any(), any())).thenReturn(0);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        service.startInstance(instanceId, "request-name");

        verify(installScriptRunner).runScript(eq(mergedDir), eq("printf 'ok' > .install-complete"), any());
        verify(registryService).recordInstallCompleted(workspace, instanceId);

        ArgumentCaptor<DockerContainerCreateRequest> captor = ArgumentCaptor.forClass(DockerContainerCreateRequest.class);
        verify(dockerService).createContainer(captor.capture());
        DockerContainerCreateRequest request = captor.getValue();
        assertThat(request.image()).isEqualTo("ghcr.io/spookly/hytale:registry");
        assertThat(request.command()).containsExactly("java", "-jar", "server.jar");
        assertThat(request.name()).isEqualTo("kodama-instance-" + instanceId);
        assertThat(request.workingDir()).isEqualTo("/workspace");
        assertThat(request.env()).contains("INSTANCE_ID=" + instanceId, "NODE_NAME=node-1", "ENV=prod");
        assertThat(request.volumeMounts()).hasSize(1);
        DockerVolumeMount mount = request.volumeMounts().get(0);
        assertThat(mount.hostPath()).isEqualTo(mergedDir.toString());
        assertThat(mount.containerPath()).isEqualTo("/workspace");
        assertThat(request.portBindings()).hasSize(1);

        var order = inOrder(dockerService, registryService);
        order.verify(registryService).recordContainerId(workspace, instanceId, "container-1", "starting");
        order.verify(dockerService).startContainer("container-1");
        order.verify(registryService).recordContainerStatus(workspace, instanceId, "running", null, null);
    }

    @Test
    void startInstanceSkipsInstallWhenRegistryAlreadyMarkedInstalled() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                "exit 99",
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(portBindingsResolver.resolveBindings(registry)).thenReturn(List.of());
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        service.startInstance(instanceId, "request-name");

        verify(installScriptRunner, never()).runScript(any(), any(), any());
        verify(registryService, never()).recordInstallCompleted(any(), any());
    }

    @Test
    void startInstanceRunsInstallScriptOnlyOnFirstStart() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry pendingInstall = registryEntry(
                instanceId,
                instanceRoot,
                false,
                "touch .install-sentinel",
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );
        InstanceRegistryEntry installComplete = registryEntry(
                instanceId,
                instanceRoot,
                true,
                pendingInstall.installScript(),
                pendingInstall.containerImage(),
                pendingInstall.startCommand()
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(pendingInstall, installComplete, installComplete);
        when(portBindingsResolver.resolveBindings(installComplete)).thenReturn(List.of());
        when(dockerService.createContainer(any()))
                .thenReturn(
                        new DockerContainerCreateResult("container-1", List.of()),
                        new DockerContainerCreateResult("container-2", List.of())
                );
        when(installScriptRunner.runScript(any(), any(), any())).thenReturn(0);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        service.startInstance(instanceId, "first-start");
        service.startInstance(instanceId, "second-start");

        verify(installScriptRunner, times(1))
                .runScript(eq(mergedDir), eq("touch .install-sentinel"), any());
        verify(registryService, times(1)).recordInstallCompleted(workspace, instanceId);
        verify(dockerService, times(2)).createContainer(any());
        verify(dockerService).startContainer("container-1");
        verify(dockerService).startContainer("container-2");
    }

    @Test
    void startInstanceRollsBackRegistryWhenDockerStartFails() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                null,
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(portBindingsResolver.resolveBindings(registry)).thenReturn(List.of());
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));
        doThrow(new RuntimeException("start failed")).when(dockerService).startContainer("container-1");

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("start failed");

        var order = inOrder(registryService, dockerService);
        order.verify(registryService).recordContainerId(workspace, instanceId, "container-1", "starting");
        order.verify(dockerService).startContainer("container-1");
        order.verify(dockerService).removeContainer("container-1", true, false);
        order.verify(registryService).clearContainerState(workspace, instanceId, "stopped", null, "start-failed");
        verify(registryService, never()).recordContainerStatus(workspace, instanceId, "running", null, null);
    }

    @Test
    void startInstanceRollsBackRegistryWhenRunningStatusUpdateFails() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                null,
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(portBindingsResolver.resolveBindings(registry)).thenReturn(List.of());
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));
        doThrow(new RuntimeException("registry status failed"))
                .when(registryService).recordContainerStatus(workspace, instanceId, "running", null, null);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("registry status failed");

        var order = inOrder(registryService, dockerService);
        order.verify(registryService).recordContainerId(workspace, instanceId, "container-1", "starting");
        order.verify(dockerService).startContainer("container-1");
        order.verify(registryService).recordContainerStatus(workspace, instanceId, "running", null, null);
        order.verify(dockerService).removeContainer("container-1", true, false);
        order.verify(registryService).clearContainerState(workspace, instanceId, "stopped", null, "start-failed");
    }

    @Test
    void startInstanceDoesNotClearRegistryWhenCleanupCannotRemoveContainer() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                null,
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(portBindingsResolver.resolveBindings(registry)).thenReturn(List.of());
        when(dockerService.createContainer(any()))
                .thenReturn(new DockerContainerCreateResult("container-1", List.of()));
        doThrow(new RuntimeException("start failed")).when(dockerService).startContainer("container-1");
        doThrow(new RuntimeException("remove failed")).when(dockerService).removeContainer("container-1", true, false);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("start failed");

        verify(registryService, never()).clearContainerState(any(), any(), any(), any(), any());
    }

    @Test
    void startInstanceFailsWhenInstallScriptFailsAndDoesNotMarkInstallCompleted() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                false,
                "exit 7",
                "ghcr.io/spookly/hytale:registry",
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);
        when(installScriptRunner.runScript(any(), any(), any())).thenReturn(7);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("exit code 7");

        verify(registryService, never()).recordInstallCompleted(any(), any());
        verify(dockerService, never()).createContainer(any());
    }

    @Test
    void startInstanceFailsFastWhenContainerImageMissingAndInstallScriptConfigured() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                false,
                "echo install",
                null,
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("Container image is required in instance registry");

        verify(installScriptRunner, never()).runScript(any(), any(), any());
        verify(registryService, never()).recordInstallCompleted(any(), any());
        verify(dockerService, never()).createContainer(any());
    }

    @Test
    void startInstanceFailsFastWhenStartCommandMissingAndInstallScriptConfigured() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                false,
                "echo install",
                "ghcr.io/spookly/hytale:registry",
                List.of()
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("Start command is required in instance registry");

        verify(installScriptRunner, never()).runScript(any(), any(), any());
        verify(registryService, never()).recordInstallCompleted(any(), any());
        verify(dockerService, never()).createContainer(any());
    }

    @Test
    void startInstanceFailsWhenContainerImageMissingInRegistry() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                null,
                null,
                List.of("java", "-jar", "server.jar")
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("Container image is required in instance registry");

        verify(installScriptRunner, never()).runScript(any(), any(), any());
        verify(dockerService, never()).createContainer(any());
    }

    @Test
    void startInstanceFailsWhenStartCommandMissingInRegistry() throws Exception {
        UUID instanceId = UUID.randomUUID();
        Path instanceRoot = tempDir.resolve("instances").resolve(instanceId.toString());
        Path mergedDir = instanceRoot.resolve("merged");
        Files.createDirectories(mergedDir);
        InstanceWorkspacePaths workspace = workspace(instanceId, instanceRoot, mergedDir);

        InstanceRegistryEntry registry = registryEntry(
                instanceId,
                instanceRoot,
                true,
                null,
                "ghcr.io/spookly/hytale:registry",
                List.of()
        );

        DockerService dockerService = mock(DockerService.class);
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        InstanceWorkspaceLayout workspaceLayout = mock(InstanceWorkspaceLayout.class);
        InstancePortBindingsResolver portBindingsResolver = mock(InstancePortBindingsResolver.class);
        InstanceInstallScriptRunner installScriptRunner = mock(InstanceInstallScriptRunner.class);

        when(workspaceLayout.resolveWorkspace(instanceId.toString())).thenReturn(workspace);
        when(registryService.loadRegistry(workspace)).thenReturn(registry);

        InstanceStartService service = new InstanceStartService(
                dockerService,
                registryService,
                workspaceLayout,
                portBindingsResolver,
                nodeConfig(),
                instanceProperties(),
                pluginRegistry(),
                installScriptRunner
        );

        assertThatThrownBy(() -> service.startInstance(instanceId, "request-name"))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("Start command is required in instance registry");

        verify(installScriptRunner, never()).runScript(any(), any(), any());
        verify(dockerService, never()).createContainer(any());
    }

    private InstanceWorkspacePaths workspace(UUID instanceId, Path instanceRoot, Path mergedDir) {
        return new InstanceWorkspacePaths(
                instanceId.toString(),
                instanceRoot,
                mergedDir,
                instanceRoot.resolve("logs"),
                instanceRoot.resolve("temp")
        );
    }

    private InstanceRegistryEntry registryEntry(
            UUID instanceId,
            Path instanceRoot,
            boolean installCompleted,
            String installScript,
            String containerImage,
            List<String> startCommand
    ) {
        return new InstanceRegistryEntry(
                instanceId,
                "instance-name",
                null,
                containerImage,
                installScript,
                startCommand,
                1,
                null,
                installCompleted,
                Map.of("ENV", "prod"),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                instanceRoot.toAbsolutePath().normalize().toString()
        );
    }

    private NodeConfig nodeConfig() {
        NodeConfig config = new NodeConfig();
        config.setNodeName("node-1");
        return config;
    }

    private InstanceProperties instanceProperties() {
        InstanceProperties instanceProperties = new InstanceProperties();
        instanceProperties.getInstanceRuntime().setWorkspaceMountPath("/workspace");
        return instanceProperties;
    }

    private NodePluginRegistry pluginRegistry() {
        NodePluginsProperties pluginsProperties = new NodePluginsProperties();
        return new NodePluginRegistry(pluginsProperties);
    }
}
