package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import net.spookly.kodama.brain.config.InstanceStaleDetectionProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
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
class InstanceStaleMonitorServiceTest {

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

    private InstanceStaleMonitorService monitorService;

    @BeforeEach
    void setUp() {
        InstanceStaleDetectionProperties properties = new InstanceStaleDetectionProperties();
        properties.setPreparingTimeoutSeconds(60);
        properties.setStartingTimeoutSeconds(120);
        InstanceStateMachine instanceStateMachine = new InstanceStateMachine(instanceEventRepository);
        monitorService = new InstanceStaleMonitorService(instanceRepository, instanceStateMachine, properties);
    }

    @Test
    void markStaleInstancesFailedMarksTimedOutPreparingAndStarting() {
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        Instance stalePreparing = instanceRepository.save(buildInstance(
                "instance-preparing-stale",
                InstanceState.PREPARING,
                now.minusSeconds(61)
        ));
        Instance staleStarting = instanceRepository.save(buildInstance(
                "instance-starting-stale",
                InstanceState.STARTING,
                now.minusSeconds(121)
        ));
        Instance freshPreparing = instanceRepository.save(buildInstance(
                "instance-preparing-fresh",
                InstanceState.PREPARING,
                now.minusSeconds(30)
        ));

        monitorService.markStaleInstancesFailed(now);

        Instance stalePreparingPersisted = instanceRepository.findById(stalePreparing.getId()).orElseThrow();
        Instance staleStartingPersisted = instanceRepository.findById(staleStarting.getId()).orElseThrow();
        Instance freshPreparingPersisted = instanceRepository.findById(freshPreparing.getId()).orElseThrow();

        assertThat(stalePreparingPersisted.getState()).isEqualTo(InstanceState.FAILED);
        assertThat(stalePreparingPersisted.getFailureReason()).isEqualTo("timeout");
        assertThat(stalePreparingPersisted.getUpdatedAt()).isEqualTo(now);

        assertThat(staleStartingPersisted.getState()).isEqualTo(InstanceState.FAILED);
        assertThat(staleStartingPersisted.getFailureReason()).isEqualTo("timeout");
        assertThat(staleStartingPersisted.getUpdatedAt()).isEqualTo(now);

        assertThat(freshPreparingPersisted.getState()).isEqualTo(InstanceState.PREPARING);
        assertThat(freshPreparingPersisted.getFailureReason()).isNull();

        List<InstanceEvent> preparingEvents =
                instanceEventRepository.findByInstanceOrderByTimestampAsc(stalePreparingPersisted);
        assertThat(preparingEvents).hasSize(1);
        assertThat(preparingEvents.getFirst().getType()).isEqualTo(InstanceEventType.FAILURE_TIMEOUT);

        List<InstanceEvent> startingEvents =
                instanceEventRepository.findByInstanceOrderByTimestampAsc(staleStartingPersisted);
        assertThat(startingEvents).hasSize(1);
        assertThat(startingEvents.getFirst().getType()).isEqualTo(InstanceEventType.FAILURE_TIMEOUT);

        assertThat(instanceEventRepository.findByInstanceOrderByTimestampAsc(freshPreparingPersisted)).isEmpty();
    }

    private Instance buildInstance(String name, InstanceState state, OffsetDateTime updatedAt) {
        return new Instance(
                name,
                name,
                state,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                updatedAt,
                updatedAt
        );
    }
}
