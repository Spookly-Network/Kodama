package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
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
class NodeHeartbeatMonitorServiceTest {

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
    private NodeRepository nodeRepository;

    private NodeHeartbeatMonitorService monitorService;

    @BeforeEach
    void setUp() {
        NodeProperties nodeProperties = new NodeProperties();
        nodeProperties.setHeartbeatTimeoutSeconds(60);
        monitorService = new NodeHeartbeatMonitorService(nodeRepository, nodeProperties);
    }

    @Test
    void markStaleNodesOfflineMarksNodesMissingHeartbeats() {
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime staleHeartbeat = now.minusSeconds(61);
        OffsetDateTime freshHeartbeat = now.minusSeconds(30);
        OffsetDateTime offlineHeartbeat = now.minusSeconds(120);

        Node staleNode = nodeRepository.save(new Node(
                "node-stale",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                5,
                2,
                staleHeartbeat,
                "1.0.0",
                null,
                "http://node-stale.internal"
        ));
        Node freshNode = nodeRepository.save(new Node(
                "node-fresh",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                5,
                1,
                freshHeartbeat,
                "1.0.0",
                null,
                "http://node-fresh.internal"
        ));
        Node offlineNode = nodeRepository.save(new Node(
                "node-offline",
                "eu-west-1",
                NodeStatus.OFFLINE,
                false,
                5,
                0,
                offlineHeartbeat,
                "1.0.0",
                null,
                "http://node-offline.internal"
        ));

        monitorService.markStaleNodesOffline(now);

        Node stalePersisted = nodeRepository.findById(staleNode.getId()).orElseThrow();
        Node freshPersisted = nodeRepository.findById(freshNode.getId()).orElseThrow();
        Node offlinePersisted = nodeRepository.findById(offlineNode.getId()).orElseThrow();

        assertThat(stalePersisted.getStatus()).isEqualTo(NodeStatus.OFFLINE);
        assertThat(stalePersisted.getLastHeartbeatAt()).isEqualTo(staleHeartbeat);
        assertThat(freshPersisted.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(freshPersisted.getLastHeartbeatAt()).isEqualTo(freshHeartbeat);
        assertThat(offlinePersisted.getStatus()).isEqualTo(NodeStatus.OFFLINE);
        assertThat(offlinePersisted.getLastHeartbeatAt()).isEqualTo(offlineHeartbeat);
    }
}
