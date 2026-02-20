package net.spookly.kodama.brain.domain.instance;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "group_template_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupTemplateAssignment {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private InstanceGroup group;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private Template template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_version_id")
  private TemplateVersion templateVersion;

  @Column(nullable = false)
  private int priority;

  public GroupTemplateAssignment(
      InstanceGroup group, Template template, TemplateVersion templateVersion, int priority) {
    this.group = Objects.requireNonNull(group, "group");
    this.template = Objects.requireNonNull(template, "template");
    this.templateVersion = templateVersion;
    this.priority = priority;
  }
}
