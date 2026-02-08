package net.spookly.kodama.brain.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class CreateInstanceRequest {

    @NonNull
    @NotBlank
    private String name;

    private String displayName;

    private UUID requestedBy;

    private UUID nodeId;

    private String region;

    private String tags;

    private Boolean devModeAllowed;

    @NonNull
    @NotEmpty
    @Valid
    @JsonAlias("templateAssignments")
    private List<TemplateAssignmentRequest> templateLayers;

    private Map<String, String> variables;

    private String variablesJson;

    private String portsJson;
}
