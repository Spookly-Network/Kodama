package net.spookly.kodama.brain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.node.NodeStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeHeartbeatRequest {

    @NotNull
    private NodeStatus status;

    @Min(0)
    private int usedSlots;
}
