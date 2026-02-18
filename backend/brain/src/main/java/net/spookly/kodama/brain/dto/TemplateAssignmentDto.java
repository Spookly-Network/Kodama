package net.spookly.kodama.brain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.blueprint.BlueprintTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.GroupTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAssignmentDto {

    private UUID id;
    private UUID templateId;
    private UUID templateVersionId;
    private int priority;

    public static TemplateAssignmentDto fromEntity(InstanceTemplateAssignment assignment) {
        return new TemplateAssignmentDto(
                assignment.getId(),
                assignment.getTemplate().getId(),
                assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                assignment.getPriority()
        );
    }

    public static TemplateAssignmentDto fromEntity(GroupTemplateAssignment assignment) {
        return new TemplateAssignmentDto(
                assignment.getId(),
                assignment.getTemplate().getId(),
                assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                assignment.getPriority()
        );
    }

    public static TemplateAssignmentDto fromEntity(BlueprintTemplateAssignment assignment) {
        return new TemplateAssignmentDto(
                assignment.getId(),
                assignment.getTemplate().getId(),
                assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                assignment.getPriority()
        );
    }
}
