package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.blueprint.BlueprintTemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlueprintTemplateAssignmentRepository
        extends JpaRepository<@NonNull BlueprintTemplateAssignment, @NonNull UUID> {

    @Query("""
            select a
            from BlueprintTemplateAssignment a
            where a.blueprint.id = :blueprintId
            order by a.priority asc, a.id asc
            """)
    List<BlueprintTemplateAssignment> findAllByBlueprintId(@Param("blueprintId") UUID blueprintId);
}
