package net.spookly.kodama.brain.repository;

import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<@NonNull User, @NonNull UUID> {
}
