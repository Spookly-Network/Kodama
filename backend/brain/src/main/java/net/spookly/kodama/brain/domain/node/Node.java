package net.spookly.kodama.brain.domain.node;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "nodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Node {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NodeStatus status;

    @Column(name = "dev_mode", nullable = false)
    private boolean devMode;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots;

    @Column(name = "used_slots", nullable = false)
    private int usedSlots;

    @Column(name = "last_heartbeat_at", nullable = false)
    private OffsetDateTime lastHeartbeatAt;

    @Column(name = "node_version", nullable = false, length = 64)
    private String nodeVersion;

    @Lob
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    public Node(
            String name,
            String region,
            NodeStatus status,
            boolean devMode,
            int capacitySlots,
            int usedSlots,
            OffsetDateTime lastHeartbeatAt,
            String nodeVersion,
            String tags,
            String baseUrl) {
        validateSlotCounts(capacitySlots, usedSlots);
        this.name = Objects.requireNonNull(name, "name");
        this.region = Objects.requireNonNull(region, "region");
        this.status = Objects.requireNonNull(status, "status");
        this.devMode = devMode;
        this.capacitySlots = capacitySlots;
        this.usedSlots = usedSlots;
        this.lastHeartbeatAt = Objects.requireNonNull(lastHeartbeatAt, "lastHeartbeatAt");
        this.nodeVersion = Objects.requireNonNull(nodeVersion, "nodeVersion");
        this.tags = tags;
        this.baseUrl = baseUrl;
    }

    public void updateRegistration(
            String region,
            boolean devMode,
            int capacitySlots,
            String nodeVersion,
            String tags,
            NodeStatus status,
            String baseUrl) {
        validateSlotCounts(capacitySlots, usedSlots);
        this.region = Objects.requireNonNull(region, "region");
        this.devMode = devMode;
        this.capacitySlots = capacitySlots;
        this.nodeVersion = Objects.requireNonNull(nodeVersion, "nodeVersion");
        this.tags = tags;
        this.baseUrl = baseUrl;
        if (status != null) {
            this.status = status;
        }
    }

    public void updateHeartbeat(NodeStatus status, int usedSlots, OffsetDateTime lastHeartbeatAt) {
        validateSlotCounts(this.capacitySlots, usedSlots);
        this.status = Objects.requireNonNull(status, "status");
        this.usedSlots = usedSlots;
        this.lastHeartbeatAt = Objects.requireNonNull(lastHeartbeatAt, "lastHeartbeatAt");
    }

    public void markOffline() {
        this.status = NodeStatus.OFFLINE;
    }

    private void validateSlotCounts(int capacitySlots, int usedSlots) {
        if (capacitySlots < 1) {
            throw new IllegalArgumentException("capacitySlots must be positive");
        }
        if (usedSlots < 0) {
            throw new IllegalArgumentException("usedSlots cannot be negative");
        }
        if (usedSlots > capacitySlots) {
            throw new IllegalArgumentException("usedSlots cannot exceed capacitySlots");
        }
    }
}
