package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintDto {

  private UUID id;
  private String name;
  private boolean permanent;
  private Integer slotsRequired;
  private String containerImage;
  private String installScript;
  private List<String> startCommand;
  private String variablesJson;
  private OffsetDateTime deletedAt;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public static BlueprintDto fromEntity(Blueprint blueprint, List<String> startCommand) {
    return new BlueprintDto(
        blueprint.getId(),
        blueprint.getName(),
        blueprint.isPermanent(),
        blueprint.getSlotsRequired(),
        blueprint.getContainerImage(),
        blueprint.getInstallScript(),
        startCommand,
        blueprint.getVariablesJson(),
        blueprint.getDeletedAt(),
        blueprint.getCreatedAt(),
        blueprint.getUpdatedAt());
  }
}
