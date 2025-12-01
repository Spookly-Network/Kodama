package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateDto {

    private UUID id;
    private String name;
    private String description;
    private TemplateType type;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    public static TemplateDto fromEntity(Template template) {
        return new TemplateDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getType(),
                template.getCreatedAt(),
                template.getCreatedBy()
        );
    }
}
