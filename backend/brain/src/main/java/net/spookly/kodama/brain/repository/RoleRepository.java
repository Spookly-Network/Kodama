package net.spookly.kodama.brain.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.user.RoleEntity;
import net.spookly.kodama.brain.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<@NonNull RoleEntity, @NonNull UUID> {

    Optional<RoleEntity> findByName(Role name);
}
