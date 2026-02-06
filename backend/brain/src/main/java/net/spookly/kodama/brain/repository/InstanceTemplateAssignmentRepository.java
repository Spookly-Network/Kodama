package net.spookly.kodama.brain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface InstanceTemplateAssignmentRepository
        extends JpaRepository<@NonNull InstanceTemplateAssignment, @NonNull UUID> {

    @Query("select a from InstanceTemplateAssignment a where a.instance.id = :instanceId order by a.priority asc, a.id asc")
    List<InstanceTemplateAssignment> findAllByInstanceId(@Param("instanceId") UUID instanceId);

    @Query("select a from InstanceTemplateAssignment a where a.instance.id in :instanceIds")
    List<InstanceTemplateAssignment> findAllByInstanceIds(@Param("instanceIds") Collection<UUID> instanceIds);
}
