package net.spookly.kodama.brain.domain.instance;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "instance_template_layers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_instance_order", columnNames = {"instance_id", "order_index"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceTemplateLayer {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private TemplateVersion templateVersion;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public InstanceTemplateLayer(Instance instance, TemplateVersion templateVersion, int orderIndex) {
        this.instance = instance;
        this.templateVersion = templateVersion;
        this.orderIndex = orderIndex;
    }
}
