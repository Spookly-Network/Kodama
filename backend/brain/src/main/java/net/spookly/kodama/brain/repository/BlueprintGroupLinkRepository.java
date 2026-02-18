package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLinkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlueprintGroupLinkRepository
        extends JpaRepository<@NonNull BlueprintGroupLink, @NonNull BlueprintGroupLinkId> {

    @Query("select l from BlueprintGroupLink l where l.blueprint.id = :blueprintId order by l.group.id asc")
    List<BlueprintGroupLink> findAllByBlueprintId(@Param("blueprintId") UUID blueprintId);
}
