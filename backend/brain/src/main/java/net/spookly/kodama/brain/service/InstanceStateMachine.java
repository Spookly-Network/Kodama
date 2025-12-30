package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstanceStateMachine {

    private static final Set<InstanceState> TERMINAL_STATES =
            EnumSet.of(InstanceState.DESTROYED, InstanceState.FAILED);

    private final InstanceEventRepository instanceEventRepository;
    private final Map<InstanceState, Set<InstanceState>> allowedTransitions;

    public InstanceStateMachine(InstanceEventRepository instanceEventRepository) {
        this.instanceEventRepository = instanceEventRepository;
        this.allowedTransitions = buildAllowedTransitions();
    }

    public void transition(
            Instance instance,
            InstanceState targetState,
            InstanceEventType eventType,
            OffsetDateTime timestamp
    ) {
        transition(instance, targetState, eventType, timestamp, null);
    }

    public void transition(
            Instance instance,
            InstanceState targetState,
            InstanceEventType eventType,
            OffsetDateTime timestamp,
            String failureReason
    ) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(targetState, "targetState");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(timestamp, "timestamp");

        InstanceState currentState = instance.getState();
        validateTransition(currentState, targetState);
        if (failureReason != null && targetState != InstanceState.FAILED) {
            throw new IllegalArgumentException("failureReason is only valid for FAILED transitions");
        }

        applyTransition(instance, targetState, timestamp, failureReason);
        instanceEventRepository.save(new InstanceEvent(instance, timestamp, eventType, null));
    }

    private Map<InstanceState, Set<InstanceState>> buildAllowedTransitions() {
        EnumMap<InstanceState, Set<InstanceState>> transitions = new EnumMap<>(InstanceState.class);
        transitions.put(InstanceState.REQUESTED, EnumSet.of(InstanceState.PREPARING));
        transitions.put(InstanceState.PREPARING, EnumSet.of(InstanceState.STARTING));
        transitions.put(InstanceState.STARTING, EnumSet.of(InstanceState.RUNNING));
        transitions.put(InstanceState.RUNNING, EnumSet.of(InstanceState.STOPPING));
        transitions.put(InstanceState.STOPPING, EnumSet.of(InstanceState.DESTROYED, InstanceState.STOPPED));
        transitions.put(InstanceState.STOPPED, EnumSet.of(InstanceState.DESTROYED, InstanceState.STARTING));

        for (InstanceState state : InstanceState.values()) {
            if (TERMINAL_STATES.contains(state)) {
                continue;
            }
            transitions.computeIfAbsent(state, key -> EnumSet.noneOf(InstanceState.class))
                    .add(InstanceState.FAILED);
        }

        return transitions;
    }

    private void validateTransition(InstanceState currentState, InstanceState targetState) {
        if (currentState == null) {
            throw new InvalidInstanceStateTransitionException("Instance state is not set");
        }
        if (currentState == targetState) {
            throw new InvalidInstanceStateTransitionException("Instance is already in state " + currentState);
        }
        Set<InstanceState> allowedTargets = allowedTransitions.getOrDefault(currentState, Set.of());
        if (!allowedTargets.contains(targetState)) {
            throw new InvalidInstanceStateTransitionException(
                    "Invalid instance state transition from " + currentState + " to " + targetState
            );
        }
    }

    private void applyTransition(
            Instance instance,
            InstanceState targetState,
            OffsetDateTime timestamp,
            String failureReason
    ) {
        switch (targetState) {
            case PREPARING -> instance.markPreparing(timestamp);
            case STARTING -> instance.markStarting(timestamp);
            case RUNNING -> instance.markRunning(timestamp);
            case STOPPING -> instance.markStopping(timestamp);
            case STOPPED -> instance.markStopped(timestamp);
            case DESTROYED -> instance.markDestroyed(timestamp);
            case FAILED -> instance.markFailed(timestamp, failureReason);
            default -> throw new InvalidInstanceStateTransitionException(
                    "Unsupported target state " + targetState
            );
        }
    }
}
