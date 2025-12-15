package net.spookly.kodama.brain.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {

    @NotBlank
    private String name;

    private String displayName;

    private UUID requestedBy;

    private UUID nodeId;

    @NotEmpty
    @Valid
    private List<InstanceTemplateLayerRequest> templateLayers;

    private String variablesJson;

    private String portsJson;
}
