package net.spookly.kodama.brain.domain.node;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "nodes")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
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
    @Column(nullable = false)
    private NodeStatus status;

    @Column(nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(nullable = false)
    private int capacity;

    public Node(String name, String region, NodeStatus status, OffsetDateTime lastSeenAt, int capacity) {
        this.name = name;
        this.region = region;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
        this.capacity = capacity;
    }

    public void updateHeartbeat(NodeStatus status, OffsetDateTime lastSeenAt, int capacity) {
        this.status = status;
        this.lastSeenAt = lastSeenAt;
        this.capacity = capacity;
    }
}
