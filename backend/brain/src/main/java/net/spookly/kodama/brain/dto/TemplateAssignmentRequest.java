package net.spookly.kodama.brain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAssignmentRequest {

  @NotNull private UUID templateId;

  private UUID templateVersionId;

  @Min(0)
  @JsonAlias("orderIndex")
  private Integer priority;
}
