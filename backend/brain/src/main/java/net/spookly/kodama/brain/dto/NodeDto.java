package net.spookly.kodama.brain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeDto {

    private UUID id;
    private String name;
    private String region;
    private NodeStatus status;
    private OffsetDateTime lastSeenAt;
    private int capacity;

    public static NodeDto fromEntity(Node node) {
        return new NodeDto(
                node.getId(),
                node.getName(),
                node.getRegion(),
                node.getStatus(),
                node.getLastSeenAt(),
                node.getCapacity()
        );
    }
}