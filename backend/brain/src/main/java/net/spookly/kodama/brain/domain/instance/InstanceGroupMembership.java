package net.spookly.kodama.brain.domain.instance;

import java.util.Objects;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instance_group_memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceGroupMembership {

    @EmbeddedId
    private InstanceGroupMembershipId id = new InstanceGroupMembershipId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instanceId")
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private InstanceGroup group;

    public InstanceGroupMembership(Instance instance, InstanceGroup group) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.group = Objects.requireNonNull(group, "group");
        this.id = new InstanceGroupMembershipId(instance.getId(), group.getId());
    }
}
