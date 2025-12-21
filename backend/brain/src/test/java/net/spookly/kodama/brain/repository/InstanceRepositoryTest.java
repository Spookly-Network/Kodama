package net.spookly.kodama.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InstanceRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceEventRepository instanceEventRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Test
    void saveAndLoadInstanceWithNode() {
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        UUID requestedBy = UUID.fromString("00000000-0000-0000-0000-000000000042");

        Node node = new Node("node-1", "eu-west-1", NodeStatus.ONLINE, false, 10,0, now, "1.0.0", "primary,ssd", "http://node-1.internal");
        Node savedNode = nodeRepository.save(node);

        Instance instance = new Instance(
                "instance-1",
                "Instance One",
                InstanceState.RUNNING,
                requestedBy,
                savedNode,
                "eu-west-1",
                "primary,ssd",
                Boolean.TRUE,
                "{\"gamePort\":25565}",
                "{\"SEED\":\"abc\"}",
                now,
                now);

        Instance savedInstance = instanceRepository.save(instance);
        Instance persisted = instanceRepository.findById(savedInstance.getId()).orElseThrow();

        assertThat(persisted.getName()).isEqualTo("instance-1");
        assertThat(persisted.getDisplayName()).isEqualTo("Instance One");
        assertThat(persisted.getState()).isEqualTo(InstanceState.RUNNING);
        assertThat(persisted.getRequestedByUserId()).isEqualTo(requestedBy);
        assertThat(persisted.getNode().getId()).isEqualTo(savedNode.getId());
        assertThat(persisted.getRegion()).isEqualTo("eu-west-1");
        assertThat(persisted.getTags()).isEqualTo("primary,ssd");
        assertThat(persisted.getDevModeAllowed()).isTrue();
        assertThat(persisted.getPortsJson()).contains("gamePort");
        assertThat(persisted.getVariablesJson()).contains("SEED");
        assertThat(persisted.getStartedAt()).isNull();
        assertThat(persisted.getStoppedAt()).isNull();
        assertThat(persisted.getFailureReason()).isNull();
    }

    @Test
    void saveAndLoadInstanceEvent() {
        OffsetDateTime createdAt = OffsetDateTime.of(2025, 1, 2, 8, 30, 0, 0, ZoneOffset.UTC);
        UUID requestedBy = UUID.fromString("00000000-0000-0000-0000-000000000043");

        Instance instance = new Instance(
                "instance-2",
                "Instance Two",
                InstanceState.REQUESTED,
                requestedBy,
                null,
                null,
                null,
                null,
                null,
                null,
                createdAt,
                createdAt);

        Instance savedInstance = instanceRepository.save(instance);

        InstanceEvent event = new InstanceEvent(
                savedInstance,
                OffsetDateTime.of(2025, 1, 2, 8, 31, 0, 0, ZoneOffset.UTC),
                InstanceEventType.REQUEST_RECEIVED,
                "{\"reason\":\"user-request\"}");

        InstanceEvent savedEvent = instanceEventRepository.save(event);
        InstanceEvent persistedEvent = instanceEventRepository.findById(savedEvent.getId()).orElseThrow();

        List<InstanceEvent> events =
                instanceEventRepository.findByInstanceOrderByTimestampAsc(savedInstance);

        assertThat(persistedEvent.getInstance().getId()).isEqualTo(savedInstance.getId());
        assertThat(persistedEvent.getType()).isEqualTo(InstanceEventType.REQUEST_RECEIVED);
        assertThat(persistedEvent.getPayloadJson()).contains("user-request");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getId()).isEqualTo(savedEvent.getId());
    }
}
