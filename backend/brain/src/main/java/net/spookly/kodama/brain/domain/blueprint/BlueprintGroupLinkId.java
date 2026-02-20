package net.spookly.kodama.brain.domain.blueprint;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class BlueprintGroupLinkId implements Serializable {

  @Column(name = "blueprint_id", nullable = false)
  private UUID blueprintId;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  public BlueprintGroupLinkId(UUID blueprintId, UUID groupId) {
    this.blueprintId = Objects.requireNonNull(blueprintId, "blueprintId");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    BlueprintGroupLinkId that = (BlueprintGroupLinkId) other;
    return Objects.equals(blueprintId, that.blueprintId) && Objects.equals(groupId, that.groupId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blueprintId, groupId);
  }
}
