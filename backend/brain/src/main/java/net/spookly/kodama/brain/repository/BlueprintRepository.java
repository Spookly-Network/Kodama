package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlueprintRepository extends JpaRepository<@NonNull Blueprint, @NonNull UUID> {

  Optional<Blueprint> findByName(String name);

  Optional<Blueprint> findByIdAndDeletedAtIsNull(UUID id);

  List<Blueprint> findAllByDeletedAtIsNullOrderByCreatedAtAsc();
}
