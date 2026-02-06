package net.spookly.kodama.brain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.service.ResolvedTemplateLayer;
import net.spookly.kodama.brain.domain.instance.TemplateAssignmentSource;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceTemplateLayerDto {

    private UUID id;
    private UUID templateId;
    private UUID templateVersionId;
    private int priority;
    private int orderIndex;
    private TemplateAssignmentSource source;

    public static InstanceTemplateLayerDto fromResolved(ResolvedTemplateLayer layer) {
        return new InstanceTemplateLayerDto(
                layer.assignmentId(),
                layer.templateId(),
                layer.templateVersion().getId(),
                layer.priority(),
                layer.orderIndex(),
                layer.source()
        );
    }
}
