package net.spookly.kodama.brain.repository;

import net.spookly.kodama.brain.domain.node.Node;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NodeRepository extends JpaRepository<Node, UUID> {

    Optional<Node> findByName(String name);
}
