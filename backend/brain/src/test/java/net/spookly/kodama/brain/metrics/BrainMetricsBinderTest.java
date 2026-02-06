package net.spookly.kodama.brain.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
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
class BrainMetricsBinderTest {

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
    private NodeRepository nodeRepository;

    @Test
    void gaugesExposeInstanceAndNodeCounts() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        Node onlineNode = new Node(
                "node-online",
                "eu-central-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node-online.internal"
        );
        Node offlineNode = new Node(
                "node-offline",
                "us-east-1",
                NodeStatus.OFFLINE,
                false,
                2,
                0,
                now,
                "1.0.0",
                null,
                "http://node-offline.internal"
        );
        nodeRepository.saveAll(List.of(onlineNode, offlineNode));

        Instance runningInstance = new Instance(
                "inst-1",
                "Instance 1",
                InstanceState.RUNNING,
                null,
                onlineNode,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
        Instance requestedInstance = new Instance(
                "inst-2",
                "Instance 2",
                InstanceState.REQUESTED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
        instanceRepository.saveAll(List.of(runningInstance, requestedInstance));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BrainMetricsBinder binder = new BrainMetricsBinder(instanceRepository, nodeRepository);
        binder.bindTo(registry);

        assertThat(registry.get("kodama.instances.state")
                .tag("state", "RUNNING")
                .gauge()
                .value()).isEqualTo(1.0);
        assertThat(registry.get("kodama.instances.state")
                .tag("state", "REQUESTED")
                .gauge()
                .value()).isEqualTo(1.0);
        assertThat(registry.get("kodama.instances.state")
                .tag("state", "FAILED")
                .gauge()
                .value()).isEqualTo(0.0);

        assertThat(registry.get("kodama.nodes.status")
                .tag("status", "ONLINE")
                .gauge()
                .value()).isEqualTo(1.0);
        assertThat(registry.get("kodama.nodes.status")
                .tag("status", "OFFLINE")
                .gauge()
                .value()).isEqualTo(1.0);
        assertThat(registry.get("kodama.nodes.status")
                .tag("status", "UNKNOWN")
                .gauge()
                .value()).isEqualTo(0.0);
    }
}
