package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.blueprint.BlueprintPortDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlueprintPortDefinitionRepository
        extends JpaRepository<@NonNull BlueprintPortDefinition, @NonNull UUID> {

    @Query("""
            select (count(p) > 0)
            from BlueprintPortDefinition p
            where p.blueprint.id = :blueprintId
              and lower(p.name) = lower(:name)
            """)
    boolean existsByBlueprintIdAndNameIgnoreCase(
            @Param("blueprintId") UUID blueprintId,
            @Param("name") String name
    );

    @Query("""
            select p
            from BlueprintPortDefinition p
            where p.blueprint.id = :blueprintId
            order by p.id asc
            """)
    List<BlueprintPortDefinition> findAllByBlueprintId(@Param("blueprintId") UUID blueprintId);
}
