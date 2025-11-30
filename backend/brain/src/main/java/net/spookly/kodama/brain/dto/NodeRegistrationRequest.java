package net.spookly.kodama.brain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.spookly.kodama.brain.domain.node.NodeStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NodeRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String region;

    @NotNull
    private NodeStatus status;

    @Min(1)
    private int capacity;
}
