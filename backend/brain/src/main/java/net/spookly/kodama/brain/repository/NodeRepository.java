package net.spookly.kodama.brain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<@NonNull Node, @NonNull UUID> {

    Optional<Node> findByName(String name);

    List<Node> findByStatusNotAndLastHeartbeatAtBefore(NodeStatus status, OffsetDateTime threshold);
}
