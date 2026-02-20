package net.spookly.kodama.nodeagent.heartbeat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.junit.jupiter.api.Test;

class HeartbeatSchedulerTest {

    @Test
    void resolveUsedSlotsSumsActiveStatusesAndClampsToCapacity() {
        NodeConfig config = new NodeConfig();
        config.setCapacitySlots(5);
        NodeHeartbeatState heartbeatState = new NodeHeartbeatState();
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        when(registryService.listRegistries()).thenReturn(List.of(
                entry("starting", 2),
                entry("running", 2),
                entry("stopping", 2),
                entry("stopped", 99),
                entry(null, 3)
        ));

        HeartbeatScheduler scheduler = new HeartbeatScheduler(
                config,
                new NodeRegistrationState(),
                mock(NodeAuthTokenReader.class),
                mock(NodeHeartbeatClient.class),
                heartbeatState,
                registryService
        );

        assertThat(scheduler.resolveUsedSlots()).isEqualTo(5);
    }

    @Test
    void buildRequestDefaultsMissingSlotsRequiredToOne() {
        NodeConfig config = new NodeConfig();
        config.setCapacitySlots(10);
        NodeHeartbeatState heartbeatState = new NodeHeartbeatState();
        InstanceRegistryService registryService = mock(InstanceRegistryService.class);
        when(registryService.listRegistries()).thenReturn(List.of(
                entry("running", null),
                entry("starting", 2),
                entry("stopping", 1)
        ));

        HeartbeatScheduler scheduler = new HeartbeatScheduler(
                config,
                new NodeRegistrationState(),
                mock(NodeAuthTokenReader.class),
                mock(NodeHeartbeatClient.class),
                heartbeatState,
                registryService
        );

        NodeHeartbeatRequest request = scheduler.buildRequest();

        assertThat(request.getUsedSlots()).isEqualTo(4);
        assertThat(heartbeatState.getUsedSlots()).isEqualTo(4);
    }

    private InstanceRegistryEntry entry(String status, Integer slotsRequired) {
        return new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                "Instance Name",
                "ghcr.io/spookly/hytale:test",
                "./install.sh",
                List.of("java", "-jar", "server.jar"),
                slotsRequired,
                null,
                false,
                Map.of(),
                List.of(),
                OffsetDateTime.now(),
                "container-id",
                status,
                OffsetDateTime.now(),
                null,
                null,
                "instances/path"
        );
    }
}
