package net.spookly.kodama.brain.domain.blueprint;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "blueprint_port_definitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlueprintPortDefinition {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private Blueprint blueprint;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortProtocol protocol;

    @Column(name = "container_port", nullable = false)
    private int containerPort;

    @Column(name = "host_range_min", nullable = false)
    private int hostRangeMin;

    @Column(name = "host_range_max", nullable = false)
    private int hostRangeMax;

    @Column(name = "host_range_step", nullable = false)
    private int hostRangeStep;

    public BlueprintPortDefinition(
            Blueprint blueprint,
            String name,
            PortProtocol protocol,
            int containerPort,
            int hostRangeMin,
            int hostRangeMax,
            int hostRangeStep
    ) {
        this.blueprint = Objects.requireNonNull(blueprint, "blueprint");
        this.name = Objects.requireNonNull(name, "name");
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.containerPort = containerPort;
        this.hostRangeMin = hostRangeMin;
        this.hostRangeMax = hostRangeMax;
        this.hostRangeStep = hostRangeStep;
    }
}
