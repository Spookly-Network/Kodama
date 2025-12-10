package net.spookly.kodama.brain.repository;

import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.instance.Instance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceRepository extends JpaRepository<@NonNull Instance, @NonNull UUID> {
}
