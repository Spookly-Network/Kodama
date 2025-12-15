package net.spookly.kodama.brain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceTemplateLayerDto {

    private UUID id;
    private UUID templateVersionId;
    private int orderIndex;

    public static InstanceTemplateLayerDto fromEntity(InstanceTemplateLayer layer) {
        return new InstanceTemplateLayerDto(
                layer.getId(),
                layer.getTemplateVersion().getId(),
                layer.getOrderIndex()
        );
    }
}
