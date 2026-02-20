package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceGroupDto {

  private UUID id;
  private String name;
  private String description;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public static InstanceGroupDto fromEntity(InstanceGroup group) {
    return new InstanceGroupDto(
        group.getId(),
        group.getName(),
        group.getDescription(),
        group.getCreatedAt(),
        group.getUpdatedAt());
  }
}
