package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.NonNull;
import net.spookly.kodama.brain.config.InstanceStaleDetectionProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "instance.stale-detection",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InstanceStaleMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStaleMonitorService.class);
    private static final String FAILURE_REASON_TIMEOUT = "timeout";

    private final InstanceRepository instanceRepository;
    private final InstanceStateMachine instanceStateMachine;
    private final InstanceStaleDetectionProperties staleDetectionProperties;

    public InstanceStaleMonitorService(
            InstanceRepository instanceRepository,
            InstanceStateMachine instanceStateMachine,
            InstanceStaleDetectionProperties staleDetectionProperties
    ) {
        this.instanceRepository = instanceRepository;
        this.instanceStateMachine = instanceStateMachine;
        this.staleDetectionProperties = staleDetectionProperties;
    }

    @Scheduled(fixedDelayString = "#{@instanceStaleDetectionProperties.monitorIntervalSeconds * 1000}")
    @Transactional
    public void monitorStaleInstances() {
        try {
            markStaleInstancesFailed(OffsetDateTime.now(ZoneOffset.UTC));
        } catch (RuntimeException ex) {
            logger.error("Failed to mark stale instances failed", ex);
            throw ex;
        }
    }

    void markStaleInstancesFailed(@NonNull OffsetDateTime now) {
        markStaleInstancesFailed(
                InstanceState.PREPARING,
                staleDetectionProperties.getPreparingTimeoutSeconds(),
                now
        );
        markStaleInstancesFailed(
                InstanceState.STARTING,
                staleDetectionProperties.getStartingTimeoutSeconds(),
                now
        );
    }

    private void markStaleInstancesFailed(InstanceState state, int timeoutSeconds, OffsetDateTime now) {
        OffsetDateTime cutoff = now.minusSeconds(timeoutSeconds);
        List<Instance> staleInstances = instanceRepository.findByStateAndUpdatedAtBefore(state, cutoff);
        for (Instance instance : staleInstances) {
            if (instance.getState() != state) {
                continue;
            }
            OffsetDateTime updatedAt = instance.getUpdatedAt();
            if (updatedAt == null || updatedAt.isAfter(cutoff)) {
                continue;
            }
            instanceStateMachine.transition(
                    instance,
                    InstanceState.FAILED,
                    InstanceEventType.FAILURE_TIMEOUT,
                    now,
                    FAILURE_REASON_TIMEOUT
            );
            logger.info(
                    "Marked instance failed due to stale state instanceId={} instanceName={} state={} updatedAt={} timeoutSeconds={}",
                    instance.getId(),
                    instance.getName(),
                    state,
                    updatedAt,
                    timeoutSeconds
            );
        }
    }
}
