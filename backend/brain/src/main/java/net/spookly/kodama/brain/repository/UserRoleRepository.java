package net.spookly.kodama.brain.repository;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.user.UserRole;
import net.spookly.kodama.brain.domain.user.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<@NonNull UserRole, @NonNull UserRoleId> {
}
