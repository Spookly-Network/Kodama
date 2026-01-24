package net.spookly.kodama.brain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceRepository extends JpaRepository<@NonNull Instance, @NonNull UUID> {
    Optional<Instance> findByName(String name);
    long countByState(InstanceState state);
    List<Instance> findByStateAndUpdatedAtBefore(InstanceState state, OffsetDateTime updatedAt);
}
