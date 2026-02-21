package net.spookly.kodama.brain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortDefinitionRequest {

  @NotBlank private String name;

  @NotBlank
  @Pattern(regexp = "(?i)tcp|udp")
  private String protocol;

  @NotNull
  @Min(1)
  @Max(65535)
  private Integer containerPort;

  @NotNull @Valid private HostRangeRequest hostRange;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HostRangeRequest {

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer min;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer max;

    @NotNull
    @Min(1)
    private Integer step;
  }
}
