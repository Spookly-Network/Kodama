package net.spookly.kodama.brain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceGroupMembershipRepository
        extends JpaRepository<@NonNull InstanceGroupMembership, @NonNull InstanceGroupMembershipId> {

    @Query("select m from InstanceGroupMembership m where m.instance.id = :instanceId")
    List<InstanceGroupMembership> findAllByInstanceId(@Param("instanceId") UUID instanceId);

    @Query("select m from InstanceGroupMembership m where m.instance.id in :instanceIds")
    List<InstanceGroupMembership> findAllByInstanceIds(@Param("instanceIds") Collection<UUID> instanceIds);

    @Query("select m.group.id from InstanceGroupMembership m where m.instance.id = :instanceId")
    List<UUID> findGroupIdsByInstanceId(@Param("instanceId") UUID instanceId);
}
