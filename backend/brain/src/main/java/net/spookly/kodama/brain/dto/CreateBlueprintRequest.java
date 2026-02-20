package net.spookly.kodama.brain.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlueprintRequest {

  @NotBlank private String name;

  private Boolean permanent;

  @Min(1) private Integer slotsRequired;

  @NotBlank private String containerImage;

  private String installScript;

  @NotEmpty private List<@NotBlank String> startCommand;

  private String variablesJson;
}
