package net.spookly.kodama.brain.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceTemplateLayerRequest {

    private UUID templateVersionId;

    private UUID templateId;

    @Min(0)
    private Integer orderIndex;

    public InstanceTemplateLayerRequest(@NotNull UUID templateVersionId, int orderIndex) {
        this.templateVersionId = templateVersionId;
        this.orderIndex = orderIndex;
    }
}
