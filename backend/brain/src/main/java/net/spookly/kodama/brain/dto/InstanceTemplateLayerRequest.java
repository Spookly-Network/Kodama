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

    @NotNull
    private UUID templateVersionId;

    @Min(0)
    private int orderIndex;
}
