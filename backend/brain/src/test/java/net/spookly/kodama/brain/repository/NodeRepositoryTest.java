package net.spookly.kodama.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
class NodeRepositoryTest {

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

    @Test
    void saveAndLoadNode() {
        OffsetDateTime heartbeat = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        Node node = new Node(
                "node-1",
                "eu-central-1",
                NodeStatus.ONLINE,
                true,
                12,
                4,
                heartbeat,
                "1.2.3",
                "primary,ssd",
                "http://node-1.internal"
        );

        Node saved = nodeRepository.save(node);
        Node persisted = nodeRepository.findById(saved.getId()).orElseThrow();

        assertThat(persisted.getName()).isEqualTo("node-1");
        assertThat(persisted.getRegion()).isEqualTo("eu-central-1");
        assertThat(persisted.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(persisted.isDevMode()).isTrue();
        assertThat(persisted.getCapacitySlots()).isEqualTo(12);
        assertThat(persisted.getUsedSlots()).isEqualTo(4);
        assertThat(persisted.getLastHeartbeatAt().toInstant()).isEqualTo(heartbeat.toInstant());
        assertThat(persisted.getNodeVersion()).isEqualTo("1.2.3");
        assertThat(persisted.getTags()).contains("primary");
        assertThat(persisted.getBaseUrl()).isEqualTo("http://node-1.internal");
    }
}
