package net.spookly.kodama.brain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTemplateVersionRequest {

    @NotBlank
    private String version;

    @NotBlank
    private String checksum;

    @NotBlank
    private String s3Key;

    private String metadataJson;
}
