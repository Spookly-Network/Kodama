package net.spookly.kodama.brain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class CreateNodeRequest {

    @NotBlank
    private String name;

    @Size(max = 512)
    @NotBlank
    private String baseUrl;

    @Min(1)
    @NotBlank
    private int capacitySlots;

    private String region;

    private boolean devMode;

    private String tags;
}
