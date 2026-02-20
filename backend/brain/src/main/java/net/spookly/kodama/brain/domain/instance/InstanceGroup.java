package net.spookly.kodama.brain.domain.instance;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "instance_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceGroup {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(nullable = false)
  private String name;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  public InstanceGroup(
      String name, String description, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.name = Objects.requireNonNull(name, "name");
    this.description = description;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public void updateDetails(String name, String description, OffsetDateTime updatedAt) {
    this.name = Objects.requireNonNull(name, "name");
    this.description = description;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }
}
