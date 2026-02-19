package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.dto.NodePreparePortDefinition;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceManager;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstancePortAllocationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void allocatePicksLowestAvailablePortAndInjectsVariables() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);

        UUID existingInstanceId = UUID.randomUUID();
        InstanceWorkspacePaths existingWorkspace = workspaceManager.prepareWorkspace(existingInstanceId.toString());
        NodePrepareInstanceRequest existingRequest = new NodePrepareInstanceRequest(
                existingInstanceId,
                "existing",
                "existing",
                "image",
                null,
                List.of("run"),
                1,
                null,
                "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":25565,\"hostPort\":30000}]",
                Map.of("PORT", "30000", "PORT_GAME", "30000"),
                null,
                List.of(sampleLayer())
        );
        registryService.recordPrepared(
                existingWorkspace,
                existingRequest,
                existingRequest.layers(),
                existingRequest.variables(),
                existingRequest.portsJson()
        );

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition definition = new NodePreparePortDefinition(
                "game",
                "udp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30002, 1)
        );

        InstancePortAllocationService.PortAllocationResult result = service.allocate(
                UUID.randomUUID(),
                List.of(definition)
        );

        assertThat(result.injectedVariables()).containsEntry("PORT", "30001");
        assertThat(result.injectedVariables()).containsEntry("PORT_GAME", "30001");
        assertThat(result.portsJson()).isNotBlank();

        List<Map<String, Object>> parsed = objectMapper.readValue(
                result.portsJson(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        );
        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst())
                .containsEntry("name", "game")
                .containsEntry("protocol", "udp")
                .containsEntry("containerPort", 25565)
                .containsEntry("hostPort", 30001);
    }

    @Test
    void allocateFailsWhenRangeHasNoAvailablePort() {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);

        UUID existingInstanceId = UUID.randomUUID();
        InstanceWorkspacePaths existingWorkspace = workspaceManager.prepareWorkspace(existingInstanceId.toString());
        NodePrepareInstanceRequest existingRequest = new NodePrepareInstanceRequest(
                existingInstanceId,
                "existing",
                "existing",
                "image",
                null,
                List.of("run"),
                1,
                null,
                "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":25565,\"hostPort\":30000}]",
                Map.of("PORT", "30000"),
                null,
                List.of(sampleLayer())
        );
        registryService.recordPrepared(
                existingWorkspace,
                existingRequest,
                existingRequest.layers(),
                existingRequest.variables(),
                existingRequest.portsJson()
        );

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition definition = new NodePreparePortDefinition(
                "game",
                "udp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30000, 1)
        );

        assertThatThrownBy(() -> service.allocate(UUID.randomUUID(), List.of(definition)))
                .isInstanceOf(InstancePrepareException.class)
                .hasMessageContaining("No available host port");
    }

    @Test
    void allocateAllowsSameHostPortForDifferentProtocolsInSingleRequest() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition tcpDefinition = new NodePreparePortDefinition(
                "game-tcp",
                "tcp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30000, 1)
        );
        NodePreparePortDefinition udpDefinition = new NodePreparePortDefinition(
                "game-udp",
                "udp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30000, 1)
        );

        InstancePortAllocationService.PortAllocationResult result = service.allocate(
                UUID.randomUUID(),
                List.of(tcpDefinition, udpDefinition)
        );

        assertThat(result.injectedVariables())
                .containsEntry("PORT", "30000")
                .containsEntry("PORT_GAME_TCP", "30000")
                .containsEntry("PORT_GAME_UDP", "30000");

        List<Map<String, Object>> parsed = objectMapper.readValue(
                result.portsJson(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        );
        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0))
                .containsEntry("protocol", "tcp")
                .containsEntry("hostPort", 30000);
        assertThat(parsed.get(1))
                .containsEntry("protocol", "udp")
                .containsEntry("hostPort", 30000);
    }

    @Test
    void allocateDoesNotTreatTcpReservationAsUdpConflict() {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);

        UUID existingInstanceId = UUID.randomUUID();
        InstanceWorkspacePaths existingWorkspace = workspaceManager.prepareWorkspace(existingInstanceId.toString());
        NodePrepareInstanceRequest existingRequest = new NodePrepareInstanceRequest(
                existingInstanceId,
                "existing",
                "existing",
                "image",
                null,
                List.of("run"),
                1,
                null,
                "[{\"name\":\"game\",\"protocol\":\"tcp\",\"containerPort\":25565,\"hostPort\":30000}]",
                Map.of("PORT", "30000", "PORT_GAME", "30000"),
                null,
                List.of(sampleLayer())
        );
        registryService.recordPrepared(
                existingWorkspace,
                existingRequest,
                existingRequest.layers(),
                existingRequest.variables(),
                existingRequest.portsJson()
        );

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition definition = new NodePreparePortDefinition(
                "game",
                "udp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30000, 1)
        );

        InstancePortAllocationService.PortAllocationResult result = service.allocate(UUID.randomUUID(), List.of(definition));

        assertThat(result.injectedVariables())
                .containsEntry("PORT", "30000")
                .containsEntry("PORT_GAME", "30000");
    }

    @Test
    void allocateIgnoresVariableReservationsWhenPortsJsonIsMissing() {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);

        UUID existingInstanceId = UUID.randomUUID();
        InstanceWorkspacePaths existingWorkspace = workspaceManager.prepareWorkspace(existingInstanceId.toString());
        NodePrepareInstanceRequest existingRequest = new NodePrepareInstanceRequest(
                existingInstanceId,
                "existing",
                "existing",
                "image",
                null,
                List.of("run"),
                1,
                null,
                null,
                Map.of("PORT", "30000", "PORT_GAME", "30001"),
                null,
                List.of(sampleLayer())
        );
        registryService.recordPrepared(
                existingWorkspace,
                existingRequest,
                existingRequest.layers(),
                existingRequest.variables(),
                existingRequest.portsJson()
        );

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition definition = new NodePreparePortDefinition(
                "game",
                "tcp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30000, 1)
        );

        InstancePortAllocationService.PortAllocationResult result = service.allocate(UUID.randomUUID(), List.of(definition));

        assertThat(result.injectedVariables())
                .containsEntry("PORT", "30000")
                .containsEntry("PORT_GAME", "30000");
    }

    @Test
    void allocateRejectsLegacyObjectPortsJsonDuringReservationLoad() {
        ObjectMapper objectMapper = objectMapper();
        InstanceWorkspaceLayout layout = workspaceLayout();
        InstanceRegistryService registryService = new InstanceRegistryService(objectMapper, layout);
        InstanceWorkspaceManager workspaceManager = new InstanceWorkspaceManager(layout);

        UUID existingInstanceId = UUID.randomUUID();
        InstanceWorkspacePaths existingWorkspace = workspaceManager.prepareWorkspace(existingInstanceId.toString());
        NodePrepareInstanceRequest existingRequest = new NodePrepareInstanceRequest(
                existingInstanceId,
                "existing",
                "existing",
                "image",
                null,
                List.of("run"),
                1,
                null,
                "{\"game\":{\"containerPort\":25565,\"hostPort\":30000}}",
                Map.of(),
                null,
                List.of(sampleLayer())
        );
        registryService.recordPrepared(
                existingWorkspace,
                existingRequest,
                existingRequest.layers(),
                existingRequest.variables(),
                existingRequest.portsJson()
        );

        InstancePortAllocationService service = new InstancePortAllocationService(registryService, objectMapper);
        NodePreparePortDefinition definition = new NodePreparePortDefinition(
                "game",
                "tcp",
                25565,
                new NodePreparePortDefinition.HostRange(30000, 30001, 1)
        );

        assertThatThrownBy(() -> service.allocate(UUID.randomUUID(), List.of(definition)))
                .isInstanceOf(InstancePrepareException.class)
                .hasMessageContaining("portsJson must be a JSON array when loading reservations");
    }

    private InstanceWorkspaceLayout workspaceLayout() {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace").toString());
        return new InstanceWorkspaceLayout(config);
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private NodePrepareInstanceLayer sampleLayer() {
        return new NodePrepareInstanceLayer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1.0.0",
                "checksum",
                "templates/base.tgz",
                null,
                0
        );
    }
}
