package net.spookly.kodama.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintPortDefinition;
import net.spookly.kodama.brain.domain.blueprint.BlueprintTemplateAssignment;
import net.spookly.kodama.brain.domain.blueprint.PortProtocol;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BlueprintRepositoryTest {

  @Container private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
  }

  @Autowired private BlueprintRepository blueprintRepository;

  @Autowired private BlueprintTemplateAssignmentRepository blueprintTemplateAssignmentRepository;

  @Autowired private BlueprintPortDefinitionRepository blueprintPortDefinitionRepository;

  @Autowired private BlueprintGroupLinkRepository blueprintGroupLinkRepository;

  @Autowired private TemplateRepository templateRepository;

  @Autowired private TemplateVersionRepository templateVersionRepository;

  @Autowired private InstanceGroupRepository instanceGroupRepository;

  @Autowired private InstanceRepository instanceRepository;

  @Test
  void saveAndLoadBlueprintAndAttachmentEntities() {
    OffsetDateTime now = OffsetDateTime.of(2025, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    Template template =
        templateRepository.save(
            new Template(
                "runtime-base", "runtime base template", TemplateType.CUSTOM, now, "system"));
    TemplateVersion templateVersion =
        templateVersionRepository.save(
            new TemplateVersion(
                template,
                "v1",
                "checksum-1",
                "templates/runtime-base/v1.tar.gz",
                "{\"channel\":\"stable\"}",
                now));
    InstanceGroup group =
        instanceGroupRepository.save(new InstanceGroup("group-a", "primary group", now, now));

    Blueprint blueprint =
        blueprintRepository.save(
            new Blueprint(
                "hytale-default",
                false,
                null,
                "ghcr.io/spookly/hytale:latest",
                "echo install",
                "[\"./run.sh\"]",
                "{\"JAVA_OPTS\":\"-Xmx2G\"}",
                now,
                now));

    blueprintTemplateAssignmentRepository.save(
        new BlueprintTemplateAssignment(blueprint, template, templateVersion, 5));
    blueprintPortDefinitionRepository.save(
        new BlueprintPortDefinition(blueprint, "game", PortProtocol.TCP, 25565, 30000, 30100, 1));
    blueprintGroupLinkRepository.save(new BlueprintGroupLink(blueprint, group));

    Blueprint persistedBlueprint = blueprintRepository.findByName("hytale-default").orElseThrow();
    List<BlueprintTemplateAssignment> assignments =
        blueprintTemplateAssignmentRepository.findAllByBlueprintId(blueprint.getId());
    List<BlueprintPortDefinition> portDefinitions =
        blueprintPortDefinitionRepository.findAllByBlueprintId(blueprint.getId());
    List<BlueprintGroupLink> groupLinks =
        blueprintGroupLinkRepository.findAllByBlueprintId(blueprint.getId());

    assertThat(persistedBlueprint.getId()).isEqualTo(blueprint.getId());
    assertThat(persistedBlueprint.getSlotsRequired()).isEqualTo(1);
    assertThat(persistedBlueprint.getDeletedAt()).isNull();

    assertThat(assignments).hasSize(1);
    assertThat(assignments.get(0).getTemplate().getId()).isEqualTo(template.getId());
    assertThat(assignments.get(0).getTemplateVersion().getId()).isEqualTo(templateVersion.getId());
    assertThat(assignments.get(0).getPriority()).isEqualTo(5);

    assertThat(portDefinitions).hasSize(1);
    assertThat(portDefinitions.get(0).getName()).isEqualTo("game");
    assertThat(portDefinitions.get(0).getContainerPort()).isEqualTo(25565);
    assertThat(portDefinitions.get(0).getProtocol()).isEqualTo(PortProtocol.TCP);

    assertThat(groupLinks).hasSize(1);
    assertThat(groupLinks.get(0).getGroup().getId()).isEqualTo(group.getId());
  }

  @Test
  void saveInstanceDefaultsSlotsRequiredToOneWhenMissing() {
    OffsetDateTime now = OffsetDateTime.of(2025, 2, 2, 10, 30, 0, 0, ZoneOffset.UTC);
    Instance instance =
        new Instance(
            "instance-default-slots",
            "Instance Default Slots",
            InstanceState.REQUESTED,
            UUID.fromString("00000000-0000-0000-0000-000000000144"),
            null,
            null,
            null,
            null,
            null,
            null,
            now,
            now);

    Instance saved = instanceRepository.save(instance);
    Instance persisted = instanceRepository.findById(saved.getId()).orElseThrow();

    assertThat(persisted.getSlotsRequired()).isNull();
  }
}
