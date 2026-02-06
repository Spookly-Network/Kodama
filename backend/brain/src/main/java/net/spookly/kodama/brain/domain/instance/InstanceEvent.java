package net.spookly.kodama.brain.domain.instance;

import java.time.OffsetDateTime;
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
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "instance_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceEventType type;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    public InstanceEvent(Instance instance, OffsetDateTime timestamp, InstanceEventType type, String payloadJson) {
        this.instance = instance;
        this.timestamp = timestamp;
        this.type = type;
        this.payloadJson = payloadJson;
    }
}
