package net.spookly.kodama.brain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Min(1)
    private int capacitySlots;

    @NotBlank
    private String nodeVersion;

    private boolean devMode;

    private String tags;

    @Size(max = 512)
    private String baseUrl;

    private NodeStatus status = NodeStatus.ONLINE;
}
