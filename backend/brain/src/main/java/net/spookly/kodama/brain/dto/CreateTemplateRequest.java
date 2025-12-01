package net.spookly.kodama.brain.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.spookly.kodama.brain.domain.template.TemplateType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTemplateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private TemplateType type;

    @NotNull
    private UUID createdBy;
}
