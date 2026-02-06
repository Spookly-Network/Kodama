package net.spookly.kodama.brain.domain.instance;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class InstanceGroupMembershipId implements Serializable {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    public InstanceGroupMembershipId(UUID instanceId, UUID groupId) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        InstanceGroupMembershipId that = (InstanceGroupMembershipId) other;
        return Objects.equals(instanceId, that.instanceId) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, groupId);
    }
}
