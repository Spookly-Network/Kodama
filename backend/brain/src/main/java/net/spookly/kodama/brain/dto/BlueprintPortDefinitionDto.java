package net.spookly.kodama.brain.dto;

import java.util.Locale;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.blueprint.BlueprintPortDefinition;
import net.spookly.kodama.brain.domain.blueprint.PortProtocol;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintPortDefinitionDto {

  private UUID id;
  private String name;
  private String protocol;
  private int containerPort;
  private HostRangeDto hostRange;

  public static BlueprintPortDefinitionDto fromEntity(BlueprintPortDefinition portDefinition) {
    return new BlueprintPortDefinitionDto(
        portDefinition.getId(),
        portDefinition.getName(),
        toApiProtocol(portDefinition.getProtocol()),
        portDefinition.getContainerPort(),
        new HostRangeDto(
            portDefinition.getHostRangeMin(),
            portDefinition.getHostRangeMax(),
            portDefinition.getHostRangeStep()));
  }

  private static String toApiProtocol(PortProtocol protocol) {
    return protocol.name().toLowerCase(Locale.ROOT);
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HostRangeDto {

    private int min;
    private int max;
    private int step;
  }
}
