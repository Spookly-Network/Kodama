package net.spookly.kodama.brain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.GroupTemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupTemplateAssignmentRepository
    extends JpaRepository<@NonNull GroupTemplateAssignment, @NonNull UUID> {

  @Query(
      "select a from GroupTemplateAssignment a where a.group.id = :groupId order by a.priority asc, a.id asc")
  List<GroupTemplateAssignment> findAllByGroupId(@Param("groupId") UUID groupId);

  @Query("select a from GroupTemplateAssignment a where a.group.id in :groupIds")
  List<GroupTemplateAssignment> findAllByGroupIds(@Param("groupIds") Collection<UUID> groupIds);
}
