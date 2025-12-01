package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.template.TemplateVersion;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateVersionDto {

    private UUID id;
    private UUID templateId;
    private String version;
    private String checksum;
    private String s3Key;
    private String metadataJson;
    private OffsetDateTime createdAt;

    public static TemplateVersionDto fromEntity(TemplateVersion templateVersion) {
        return new TemplateVersionDto(
                templateVersion.getId(),
                templateVersion.getTemplate().getId(),
                templateVersion.getVersion(),
                templateVersion.getChecksum(),
                templateVersion.getS3Key(),
                templateVersion.getMetadataJson(),
                templateVersion.getCreatedAt()
        );
    }
}
