package net.spookly.kodama.brain.domain.instance;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.node.Node;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instance {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceState state;

    // Optional: present when a user explicitly requested the instance.
    @Column
    private UUID requestedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private Node node;

    @Column(name = "requested_region")
    private String region;

    @Lob
    @Column(name = "requested_tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "dev_mode_allowed")
    private Boolean devModeAllowed;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String portsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String variablesJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime stoppedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    public Instance(
            String name,
            String displayName,
            InstanceState state,
            UUID requestedByUserId,
            Node node,
            String region,
            String tags,
            Boolean devModeAllowed,
            String portsJson,
            String variablesJson,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
//            OffsetDateTime startedAt,
//            OffsetDateTime stoppedAt,
//            String failureReason
    ) {
        this.name = name;
        this.displayName = displayName;
        this.state = state;
        this.requestedByUserId = requestedByUserId;
        this.node = node;
        this.region = region;
        this.tags = tags;
        this.devModeAllowed = devModeAllowed;
        this.portsJson = portsJson;
        this.variablesJson = variablesJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
//        this.startedAt = startedAt;
//        this.stoppedAt = stoppedAt;
//        this.failureReason = failureReason;
    }

    public void markPrepared(OffsetDateTime timestamp) {
        updateLifecycle(InstanceState.PREPARED, timestamp);
        this.failureReason = null;
    }

    public void markRunning(OffsetDateTime timestamp) {
        updateLifecycle(InstanceState.RUNNING, timestamp);
        if (this.startedAt == null) {
            this.startedAt = timestamp;
        }
        this.stoppedAt = null;
        this.failureReason = null;
    }

    public void markStopped(OffsetDateTime timestamp) {
        updateLifecycle(InstanceState.STOPPED, timestamp);
        if (this.stoppedAt == null) {
            this.stoppedAt = timestamp;
        }
        this.failureReason = null;
    }

    public void markFailed(OffsetDateTime timestamp, String failureReason) {
        updateLifecycle(InstanceState.FAILED, timestamp);
        this.failureReason = failureReason;
        if (this.stoppedAt == null) {
            this.stoppedAt = timestamp;
        }
    }

    private void updateLifecycle(InstanceState state, OffsetDateTime timestamp) {
        this.state = Objects.requireNonNull(state, "state");
        this.updatedAt = Objects.requireNonNull(timestamp, "timestamp");
    }
}
