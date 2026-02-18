package net.spookly.kodama.brain.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {

    @NotBlank
    private String name;

    private String displayName;

    private UUID blueprintId;

    private UUID requestedBy;

    private UUID nodeId;

    private String region;

    private String tags;

    private Boolean devModeAllowed;

    private Boolean permanent;

    @Min(1)
    private Integer slotsRequired;

    private String containerImage;

    private String installScript;

    private List<@NotBlank String> startCommand;

    @Valid
    private List<PortDefinitionRequest> portDefinitions;

    private List<@NotNull UUID> groupIds;

    @Valid
    @JsonAlias("templateAssignments")
    private List<TemplateAssignmentRequest> templateLayers;

    private Map<String, String> variables;

    private String variablesJson;

    private String portsJson;

    public CreateInstanceRequest(String name, List<TemplateAssignmentRequest> templateLayers) {
        this.name = name;
        this.templateLayers = templateLayers;
    }
}
