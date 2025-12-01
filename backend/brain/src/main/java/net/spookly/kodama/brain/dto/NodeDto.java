package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeDto {

    private UUID id;
    private String name;
    private String region;
    private NodeStatus status;
    private boolean devMode;
    private int capacitySlots;
    private int usedSlots;
    private OffsetDateTime lastHeartbeatAt;
    private String nodeVersion;
    private String tags;

    public static NodeDto fromEntity(Node node) {
        return new NodeDto(
                node.getId(),
                node.getName(),
                node.getRegion(),
                node.getStatus(),
                node.isDevMode(),
                node.getCapacitySlots(),
                node.getUsedSlots(),
                node.getLastHeartbeatAt(),
                node.getNodeVersion(),
                node.getTags()
        );
    }
}
