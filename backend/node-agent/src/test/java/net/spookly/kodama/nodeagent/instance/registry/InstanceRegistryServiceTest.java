package net.spookly.kodama.nodeagent.instance.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                "{\"port\":25565}",
                variables,
                null,
                layers
        );

        InstanceWorkspacePaths workspace = prepareWorkspace(instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper());

        registryService.recordPrepared(workspace, request, layers, variables);

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        assertThat(registryFile).exists();

        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.instanceId()).isEqualTo(instanceId);
        assertThat(entry.name()).isEqualTo("instance-name");
        assertThat(entry.displayName()).isEqualTo("Instance Name");
        assertThat(entry.portsJson()).isEqualTo("{\"port\":25565}");
        assertThat(entry.variables()).containsEntry("REGION", "local");
        assertThat(entry.layers()).hasSize(1);
        assertThat(entry.layers().get(0).templateId()).isEqualTo(layer.templateId());
        assertThat(entry.preparedAt()).isNotNull();
        assertThat(entry.containerId()).isNull();
        assertThat(entry.containerStatus()).isNull();
        assertThat(entry.containerStatusUpdatedAt()).isNull();
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
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspacePaths workspace = prepareWorkspace("different-instance");
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper());

        assertThatThrownBy(() -> registryService.recordPrepared(workspace, request, layers, Map.of()))
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
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspacePaths workspace = prepareWorkspace(instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper());

        registryService.recordPrepared(workspace, request, layers, Map.of());
        registryService.recordContainerId(workspace, instanceId, "container-123");

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.containerId()).isEqualTo("container-123");
        assertThat(entry.containerStatus()).isEqualTo("running");
        assertThat(entry.containerStatusUpdatedAt()).isNotNull();
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
                null,
                null,
                Map.of(),
                null,
                layers
        );
        InstanceWorkspacePaths workspace = prepareWorkspace(instanceId.toString());
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper());

        registryService.recordPrepared(workspace, request, layers, Map.of());
        registryService.recordContainerId(workspace, instanceId, "container-123");
        registryService.recordContainerStatus(workspace, instanceId, "stopped");

        Path registryFile = workspace.instanceRoot().resolve("instance.json");
        InstanceRegistryEntry entry = objectMapper().readValue(registryFile.toFile(), InstanceRegistryEntry.class);
        assertThat(entry.containerId()).isEqualTo("container-123");
        assertThat(entry.containerStatus()).isEqualTo("stopped");
        assertThat(entry.containerStatusUpdatedAt()).isNotNull();
    }

    private InstanceWorkspacePaths prepareWorkspace(String instanceId) {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace-root").toString());
        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);
        return workspaceManager.prepareWorkspace(instanceId);
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
