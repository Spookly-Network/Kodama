package net.spookly.kodama.brain.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.node.Node;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<@NonNull Node, @NonNull UUID> {

    Optional<Node> findByName(String name);
}
