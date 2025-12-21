package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeHeartbeatRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationResponse;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(NodeProperties.class)
@Import(NodeService.class)
class NodeServiceTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("node.heartbeat-interval-seconds", () -> "45");
    }

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeRepository nodeRepository;

    @Test
    void registerNodeCreatesNewEntity() {
        NodeRegistrationRequest request = new NodeRegistrationRequest();
        request.setName("node-1");
        request.setRegion("eu-central-1");
        request.setCapacitySlots(10);
        request.setNodeVersion("1.0.0");
        request.setTags("primary,ssd");
        request.setBaseUrl("http://node-1.internal");

        NodeRegistrationResponse response = nodeService.registerNode(request);

        assertThat(response.getNodeId()).isNotNull();
        assertThat(response.getHeartbeatIntervalSeconds()).isEqualTo(45);

        Node persisted = nodeRepository.findById(response.getNodeId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("node-1");
        assertThat(persisted.getRegion()).isEqualTo("eu-central-1");
        assertThat(persisted.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(persisted.getCapacitySlots()).isEqualTo(10);
        assertThat(persisted.getNodeVersion()).isEqualTo("1.0.0");
        assertThat(persisted.getTags()).isEqualTo("primary,ssd");
        assertThat(persisted.getBaseUrl()).isEqualTo("http://node-1.internal");
    }

    @Test
    void registerNodeUpdatesExistingRecord() {
        NodeRegistrationRequest request = new NodeRegistrationRequest();
        request.setName("node-2");
        request.setRegion("eu-central-1");
        request.setCapacitySlots(8);
        request.setNodeVersion("1.0.0");
        request.setTags("primary");
        request.setBaseUrl("http://node-2.internal");

        NodeRegistrationResponse firstResponse = nodeService.registerNode(request);
        OffsetDateTime initialHeartbeat = nodeRepository.findById(firstResponse.getNodeId())
                .orElseThrow()
                .getLastHeartbeatAt()
                .truncatedTo(ChronoUnit.MICROS);

        NodeRegistrationRequest updateRequest = new NodeRegistrationRequest();
        updateRequest.setName("node-2");
        updateRequest.setRegion("eu-west-1");
        updateRequest.setCapacitySlots(20);
        updateRequest.setNodeVersion("2.0.0");
        updateRequest.setDevMode(true);
        updateRequest.setTags("primary,arm");
        updateRequest.setStatus(NodeStatus.OFFLINE);
        updateRequest.setBaseUrl("http://node-2-new.internal");

        NodeRegistrationResponse secondResponse = nodeService.registerNode(updateRequest);

        assertThat(secondResponse.getNodeId()).isEqualTo(firstResponse.getNodeId());

        Node updated = nodeRepository.findById(secondResponse.getNodeId()).orElseThrow();
        assertThat(updated.getRegion()).isEqualTo("eu-west-1");
        assertThat(updated.isDevMode()).isTrue();
        assertThat(updated.getCapacitySlots()).isEqualTo(20);
        assertThat(updated.getNodeVersion()).isEqualTo("2.0.0");
        assertThat(updated.getStatus()).isEqualTo(NodeStatus.OFFLINE);
        assertThat(updated.getTags()).isEqualTo("primary,arm");
        assertThat(updated.getBaseUrl()).isEqualTo("http://node-2-new.internal");
        assertThat(updated.getLastHeartbeatAt().truncatedTo(ChronoUnit.MICROS))
                .isAfterOrEqualTo(initialHeartbeat);
    }

    @Test
    void heartbeatUpdatesNodeUsageAndTimestamp() {
        OffsetDateTime initialHeartbeat = OffsetDateTime.now(ZoneOffset.UTC)
                .minusMinutes(5)
                .truncatedTo(ChronoUnit.MICROS);
        Node node = new Node(
                "node-3",
                "us-east-1",
                NodeStatus.UNKNOWN,
                false,
                6,
                1,
                initialHeartbeat,
                "1.2.3",
                "edge",
                "http://node-3.internal"
        );
        Node persisted = nodeRepository.save(node);

        NodeHeartbeatRequest request = new NodeHeartbeatRequest(NodeStatus.ONLINE, 4);

        NodeDto response = nodeService.heartbeat(persisted.getId(), request);
        Node reloaded = nodeRepository.findById(persisted.getId()).orElseThrow();

        assertThat(response.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(response.getUsedSlots()).isEqualTo(4);
        assertThat(reloaded.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(reloaded.getUsedSlots()).isEqualTo(4);
        assertThat(reloaded.getLastHeartbeatAt().truncatedTo(ChronoUnit.MICROS))
                .isAfter(initialHeartbeat);
    }

    @Test
    void heartbeatRejectsUsedSlotsExceedingCapacity() {
        Node node = new Node(
                "node-4",
                "us-west-2",
                NodeStatus.ONLINE,
                false,
                2,
                1,
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS),
                "2.0.0",
                null,
                "http://node-4.internal"
        );
        Node persisted = nodeRepository.save(node);

        NodeHeartbeatRequest request = new NodeHeartbeatRequest(NodeStatus.ONLINE, 3);

        assertThatThrownBy(() -> nodeService.heartbeat(persisted.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
