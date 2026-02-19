package net.spookly.kodama.nodeagent.instance.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceManager;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceRegistryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void recordPreparedWritesRegistryFile() throws Exception {
        UUID instanceId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "s3/key.tgz",
                "{\"type\":\"base\"}",
                0
        );
        List<NodePrepareInstanceLayer> layers = List.of(layer);
        Map<String, String> variables = Map.of("REGION", "local");
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "instance-name",
                "Instance Name",
                "ghcr.io/spookly/hytale:test",
                "./install.sh",
                List.of("java", "-jar", "server.jar"),
                2,
                null,
                "{\"port\":25565}",
                variables,
                null,
                layers
        );

        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);

        registryService.recordPrepared(workspace, request, layers, variables, request.portsJson());

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        assertThat(registryFile).exists();

        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.instanceId()).isEqualTo(instanceId);
        assertThat(entry.name()).isEqualTo("instance-name");
        assertThat(entry.displayName()).isEqualTo("Instance Name");
        assertThat(entry.containerImage()).isEqualTo("ghcr.io/spookly/hytale:test");
        assertThat(entry.installScript()).isEqualTo("./install.sh");
        assertThat(entry.startCommand()).containsExactly("java", "-jar", "server.jar");
        assertThat(entry.slotsRequired()).isEqualTo(2);
        assertThat(entry.portsJson()).isEqualTo("{\"port\":25565}");
        assertThat(entry.installCompleted()).isFalse();
        assertThat(entry.variables()).containsEntry("REGION", "local");
        assertThat(entry.layers()).hasSize(1);
        assertThat(entry.layers().get(0).templateId()).isEqualTo(layer.templateId());
        assertThat(entry.preparedAt()).isNotNull();
        assertThat(entry.containerId()).isNull();
        assertThat(entry.containerStatus()).isNull();
        assertThat(entry.containerStatusUpdatedAt()).isNull();
        assertThat(entry.containerExitCode()).isNull();
        assertThat(entry.containerExitReason()).isNull();
        assertThat(entry.workspacePath()).isEqualTo(workspace.instanceRoot().toAbsolutePath().normalize().toString());
    }

    @Test
    void recordPreparedRejectsMismatchedInstanceId() {
        UUID instanceId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "s3/key.tgz",
                null,
                0
        );
        List<NodePrepareInstanceLayer> layers = List.of(layer);
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "instance-name",
                "Instance Name",
                "ghcr.io/spookly/hytale:test",
                "./install.sh",
                List.of("java", "-jar", "server.jar"),
                2,
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, "different-instance");
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);

        assertThatThrownBy(() -> registryService.recordPrepared(workspace, request, layers, Map.of(), request.portsJson()))
                .isInstanceOf(InstanceRegistryException.class)
                .hasMessageContaining("instanceId does not match workspace");
    }

    @Test
    void recordContainerIdUpdatesRegistry() throws Exception {
        UUID instanceId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "s3/key.tgz",
                null,
                0
        );
        List<NodePrepareInstanceLayer> layers = List.of(layer);
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "instance-name",
                "Instance Name",
                "ghcr.io/spookly/hytale:test",
                "./install.sh",
                List.of("java", "-jar", "server.jar"),
                2,
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);

        registryService.recordPrepared(workspace, request, layers, Map.of(), request.portsJson());
        registryService.recordContainerId(workspace, instanceId, "container-123");

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.containerImage()).isEqualTo("ghcr.io/spookly/hytale:test");
        assertThat(entry.installScript()).isEqualTo("./install.sh");
        assertThat(entry.startCommand()).containsExactly("java", "-jar", "server.jar");
        assertThat(entry.slotsRequired()).isEqualTo(2);
        assertThat(entry.installCompleted()).isFalse();
        assertThat(entry.containerId()).isEqualTo("container-123");
        assertThat(entry.containerStatus()).isEqualTo("running");
        assertThat(entry.containerStatusUpdatedAt()).isNotNull();
        assertThat(entry.containerExitCode()).isNull();
        assertThat(entry.containerExitReason()).isNull();
        assertThat(entry.workspacePath()).isEqualTo(workspace.instanceRoot().toAbsolutePath().normalize().toString());
    }

    @Test
    void recordContainerStatusUpdatesRegistry() throws Exception {
        UUID instanceId = UUID.randomUUID();
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "s3/key.tgz",
                null,
                0
        );
        List<NodePrepareInstanceLayer> layers = List.of(layer);
        NodePrepareInstanceRequest request = new NodePrepareInstanceRequest(
                instanceId,
                "instance-name",
                "Instance Name",
                "ghcr.io/spookly/hytale:test",
                "./install.sh",
                List.of("java", "-jar", "server.jar"),
                2,
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);

        registryService.recordPrepared(workspace, request, layers, Map.of(), request.portsJson());
        registryService.recordContainerId(workspace, instanceId, "container-123");
        registryService.recordContainerStatus(workspace, instanceId, "stopped", 0, "exited");

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.containerImage()).isEqualTo("ghcr.io/spookly/hytale:test");
        assertThat(entry.installScript()).isEqualTo("./install.sh");
        assertThat(entry.startCommand()).containsExactly("java", "-jar", "server.jar");
        assertThat(entry.slotsRequired()).isEqualTo(2);
        assertThat(entry.installCompleted()).isFalse();
        assertThat(entry.containerId()).isEqualTo("container-123");
        assertThat(entry.containerStatus()).isEqualTo("stopped");
        assertThat(entry.containerStatusUpdatedAt()).isNotNull();
        assertThat(entry.containerExitCode()).isEqualTo(0);
        assertThat(entry.containerExitReason()).isEqualTo("exited");
        assertThat(entry.workspacePath()).isEqualTo(workspace.instanceRoot().toAbsolutePath().normalize().toString());
    }

    @Test
    void listRegistriesReturnsKnownInstances() {
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);
        NodePrepareInstanceLayer layer = new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "s3/key.tgz",
                null,
                0
        );
        List<NodePrepareInstanceLayer> layers = List.of(layer);

        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        InstanceWorkspacePaths firstWorkspace = prepareWorkspace(layout, firstId.toString());
        InstanceWorkspacePaths secondWorkspace = prepareWorkspace(layout, secondId.toString());

        NodePrepareInstanceRequest firstRequest = new NodePrepareInstanceRequest(
                firstId,
                "first",
                null,
                null,
                Map.of(),
                null,
                layers
        );
        NodePrepareInstanceRequest secondRequest = new NodePrepareInstanceRequest(
                secondId,
                "second",
                null,
                null,
                Map.of(),
                null,
                layers
        );

        registryService.recordPrepared(firstWorkspace, firstRequest, layers, Map.of(), firstRequest.portsJson());
        registryService.recordPrepared(secondWorkspace, secondRequest, layers, Map.of(), secondRequest.portsJson());

        List<InstanceRegistryEntry> entries = registryService.listRegistries();

        assertThat(entries)
                .extracting(InstanceRegistryEntry::instanceId)
                .containsExactlyInAnyOrder(firstId, secondId);
        assertThat(entries)
                .allMatch(entry -> entry.workspacePath() != null && !entry.workspacePath().isBlank());
        assertThat(entries)
                .allMatch(entry -> entry.variables() == null);
        assertThat(entries)
                .anyMatch(entry -> entry.workspacePath().equals(Path.of("instances", firstId.toString()).toString()))
                .anyMatch(entry -> entry.workspacePath().equals(Path.of("instances", secondId.toString()).toString()));
    }

    @Test
    void loadRegistryDefaultsMissingBlueprintRuntimeFieldsForLegacyEntries() throws Exception {
        UUID instanceId = UUID.randomUUID();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, instanceId.toString());
        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        String legacyJson = """
                {
                  "instanceId": "%s",
                  "name": "legacy",
                  "displayName": "Legacy",
                  "portsJson": "{\\"game\\":25565}",
                  "variables": {
                    "ENV": "prod"
                  },
                  "layers": [],
                  "preparedAt": null,
                  "containerId": null,
                  "containerStatus": null,
                  "containerStatusUpdatedAt": null,
                  "containerExitCode": null,
                  "containerExitReason": null,
                  "workspacePath": null
                }
                """.formatted(instanceId);
        Files.writeString(registryFile, legacyJson);

        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);
        InstanceRegistryEntry entry = registryService.loadRegistry(workspace);

        assertThat(entry.containerImage()).isNull();
        assertThat(entry.installScript()).isNull();
        assertThat(entry.startCommand()).isEmpty();
        assertThat(entry.slotsRequired()).isEqualTo(1);
        assertThat(entry.installCompleted()).isFalse();
    }

    @Test
    void loadRegistrySupportsAllocatedPortsAlias() throws Exception {
        UUID instanceId = UUID.randomUUID();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceWorkspacePaths workspace = prepareWorkspace(layout, instanceId.toString());
        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        String aliasJson = """
                {
                  "instanceId": "%s",
                  "name": "legacy",
                  "displayName": "Legacy",
                  "allocatedPorts": "{\\"game\\":25565}",
                  "variables": {},
                  "layers": [],
                  "workspacePath": null
                }
                """.formatted(instanceId);
        Files.writeString(registryFile, aliasJson);

        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper(), layout);
        InstanceRegistryEntry entry = registryService.loadRegistry(workspace);

        assertThat(entry.portsJson()).isEqualTo("{\"game\":25565}");
    }

    private InstanceWorkspacePaths prepareWorkspace(InstanceWorkspaceLayout layout, String instanceId) {
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);
        return workspaceManager.prepareWorkspace(instanceId);
    }

    private InstanceWorkspaceLayout workspaceLayout() {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace-root").toString());
        return new InstanceWorkspaceLayout(config);
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
