package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(InstanceStateMachine.class)
class InstanceStateMachineTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    private static final UUID REQUESTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String FAILURE_REASON = "node reported failure";

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
            new Transition(InstanceState.REQUESTED, InstanceState.PREPARING),
            new Transition(InstanceState.PREPARING, InstanceState.STARTING),
            new Transition(InstanceState.STARTING, InstanceState.RUNNING),
            new Transition(InstanceState.RUNNING, InstanceState.STOPPING),
            new Transition(InstanceState.STOPPING, InstanceState.DESTROYED),
            new Transition(InstanceState.STOPPING, InstanceState.STOPPED),
            new Transition(InstanceState.STOPPED, InstanceState.DESTROYED),
            new Transition(InstanceState.STOPPED, InstanceState.STARTING),
            new Transition(InstanceState.REQUESTED, InstanceState.FAILED),
            new Transition(InstanceState.PREPARING, InstanceState.FAILED),
            new Transition(InstanceState.STARTING, InstanceState.FAILED),
            new Transition(InstanceState.RUNNING, InstanceState.FAILED),
            new Transition(InstanceState.STOPPING, InstanceState.FAILED),
            new Transition(InstanceState.STOPPED, InstanceState.FAILED),
            new Transition(InstanceState.PREPARED, InstanceState.FAILED)
    );

    @Autowired
    private InstanceStateMachine instanceStateMachine;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceEventRepository instanceEventRepository;

    @ParameterizedTest
    @MethodSource("validTransitions")
    void transitionAppliesValidTransitions(
            InstanceState fromState,
            InstanceState toState,
            InstanceEventType eventType
    ) {
        Instance instance = saveInstance(fromState);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (toState == InstanceState.FAILED) {
            instanceStateMachine.transition(instance, toState, eventType, now, FAILURE_REASON);
        } else {
            instanceStateMachine.transition(instance, toState, eventType, now);
        }

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(toState);
        if (toState == InstanceState.FAILED) {
            assertThat(persisted.getFailureReason()).isEqualTo(FAILURE_REASON);
        }

        InstanceEvent event = instanceEventRepository
                .findAllByInstanceIdOrderByTimestampAsc(instance.getId())
                .getFirst();
        assertThat(event.getType()).isEqualTo(eventType);
    }

    @Test
    void transitionRejectsInvalidTransitions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        EnumSet<InstanceState> allStates = EnumSet.allOf(InstanceState.class);

        for (InstanceState fromState : allStates) {
            for (InstanceState toState : allStates) {
                if (fromState == toState) {
                    continue;
                }
                if (ALLOWED_TRANSITIONS.contains(new Transition(fromState, toState))) {
                    continue;
                }

                Instance instance = buildInstance(fromState);
                assertThatThrownBy(() ->
                        instanceStateMachine.transition(instance, toState, InstanceEventType.FAILURE_REPORTED, now)
                ).isInstanceOf(InvalidInstanceStateTransitionException.class);
            }
        }
    }

    private Instance saveInstance(InstanceState state) {
        return instanceRepository.save(buildInstance(state));
    }

    private Instance buildInstance(InstanceState state) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Instance(
                "instance-" + UUID.randomUUID(),
                "Instance " + state,
                state,
                REQUESTER_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(InstanceState.REQUESTED, InstanceState.PREPARING, InstanceEventType.PREPARE_DISPATCHED),
                Arguments.of(InstanceState.PREPARING, InstanceState.STARTING, InstanceEventType.PREPARE_COMPLETED),
                Arguments.of(InstanceState.STARTING, InstanceState.RUNNING, InstanceEventType.START_COMPLETED),
                Arguments.of(InstanceState.RUNNING, InstanceState.STOPPING, InstanceEventType.STOP_DISPATCHED),
                Arguments.of(InstanceState.STOPPING, InstanceState.STOPPED, InstanceEventType.STOP_COMPLETED),
                Arguments.of(InstanceState.STOPPING, InstanceState.DESTROYED, InstanceEventType.DESTROY_COMPLETED),
                Arguments.of(InstanceState.STOPPED, InstanceState.DESTROYED, InstanceEventType.DESTROY_COMPLETED),
                Arguments.of(InstanceState.STOPPED, InstanceState.STARTING, InstanceEventType.START_DISPATCHED),
                Arguments.of(InstanceState.REQUESTED, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.PREPARING, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.STARTING, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.RUNNING, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.STOPPING, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.STOPPED, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED),
                Arguments.of(InstanceState.PREPARED, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED)
        );
    }

    private record Transition(InstanceState from, InstanceState to) {
    }
}
