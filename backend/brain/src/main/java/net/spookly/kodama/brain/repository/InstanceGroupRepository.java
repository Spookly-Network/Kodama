package net.spookly.kodama.brain.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceGroupRepository
    extends JpaRepository<@NonNull InstanceGroup, @NonNull UUID> {

  Optional<InstanceGroup> findByName(String name);
}
