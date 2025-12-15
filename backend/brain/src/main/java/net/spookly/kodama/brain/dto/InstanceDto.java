package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDto {

    private UUID id;
    private String name;
    private String displayName;
    private InstanceState state;
    private UUID nodeId;
    private UUID requestedBy;
    private String portsJson;
    private String variablesJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime stoppedAt;
    private String failureReason;
    private List<InstanceTemplateLayerDto> templateLayers;

    public static InstanceDto fromEntity(Instance instance, List<InstanceTemplateLayer> layers) {
        List<InstanceTemplateLayerDto> layerDtos = layers.stream()
                .map(InstanceTemplateLayerDto::fromEntity)
                .toList();

        return new InstanceDto(
                instance.getId(),
                instance.getName(),
                instance.getDisplayName(),
                instance.getState(),
                instance.getNode() == null ? null : instance.getNode().getId(),
                instance.getRequestedByUserId(),
                instance.getPortsJson(),
                instance.getVariablesJson(),
                instance.getCreatedAt(),
                instance.getUpdatedAt(),
                instance.getStartedAt(),
                instance.getStoppedAt(),
                instance.getFailureReason(),
                layerDtos
        );
    }
}
