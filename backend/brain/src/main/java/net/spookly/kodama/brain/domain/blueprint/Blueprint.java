package net.spookly.kodama.brain.domain.blueprint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "blueprints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Blueprint {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private boolean permanent;

  @Column(name = "slots_required", nullable = false)
  private Integer slotsRequired;

  @Column(name = "container_image", nullable = false)
  private String containerImage;

  @Lob
  @Column(name = "install_script", columnDefinition = "TEXT")
  private String installScript;

  @Lob
  @Column(name = "start_command_json", columnDefinition = "TEXT", nullable = false)
  private String startCommandJson;

  @Lob
  @Column(name = "variables_json", columnDefinition = "TEXT")
  private String variablesJson;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Blueprint(
      String name,
      boolean permanent,
      Integer slotsRequired,
      String containerImage,
      String installScript,
      String startCommandJson,
      String variablesJson,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this.name = Objects.requireNonNull(name, "name");
    this.permanent = permanent;
    this.slotsRequired = slotsRequired;
    this.containerImage = Objects.requireNonNull(containerImage, "containerImage");
    this.installScript = installScript;
    this.startCommandJson = Objects.requireNonNull(startCommandJson, "startCommandJson");
    this.variablesJson = variablesJson;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  @PrePersist
  private void applyDefaultsOnPersist() {
    if (slotsRequired == null) {
      slotsRequired = 1;
    }
  }

  public void update(
      String name,
      boolean permanent,
      Integer slotsRequired,
      String containerImage,
      String installScript,
      String startCommandJson,
      String variablesJson,
      OffsetDateTime updatedAt) {
    this.name = Objects.requireNonNull(name, "name");
    this.permanent = permanent;
    this.slotsRequired = slotsRequired == null ? 1 : slotsRequired;
    this.containerImage = Objects.requireNonNull(containerImage, "containerImage");
    this.installScript = installScript;
    this.startCommandJson = Objects.requireNonNull(startCommandJson, "startCommandJson");
    this.variablesJson = variablesJson;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public void softDelete(OffsetDateTime deletedAt) {
    OffsetDateTime timestamp = Objects.requireNonNull(deletedAt, "deletedAt");
    this.deletedAt = timestamp;
    this.updatedAt = timestamp;
  }
}
