package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.repository.BlueprintGroupLinkRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BlueprintGroupLinkService.class)
class BlueprintGroupLinkServiceTest {

  @Container private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
  }

  @Autowired private BlueprintGroupLinkService blueprintGroupLinkService;

  @Autowired private BlueprintRepository blueprintRepository;

  @Autowired private InstanceGroupRepository instanceGroupRepository;

  @Autowired private BlueprintGroupLinkRepository blueprintGroupLinkRepository;

  @Test
  void addGroupLinkIsIdempotentAndListsGroups() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Blueprint blueprint = createBlueprint("bp-group-links", now);
    InstanceGroup group = createGroup("group-a", now);

    blueprintGroupLinkService.addGroupLink(blueprint.getId(), group.getId());
    blueprintGroupLinkService.addGroupLink(blueprint.getId(), group.getId());

    assertThat(blueprintGroupLinkRepository.findAllByBlueprintId(blueprint.getId())).hasSize(1);

    List<InstanceGroupDto> linkedGroups =
        blueprintGroupLinkService.listGroupLinks(blueprint.getId());
    assertThat(linkedGroups).extracting(InstanceGroupDto::getId).containsExactly(group.getId());
  }

  @Test
  void removeGroupLinkIsIdempotent() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Blueprint blueprint = createBlueprint("bp-group-remove", now);
    InstanceGroup group = createGroup("group-b", now);

    blueprintGroupLinkService.addGroupLink(blueprint.getId(), group.getId());
    blueprintGroupLinkService.removeGroupLink(blueprint.getId(), group.getId());

    assertThat(blueprintGroupLinkRepository.findAllByBlueprintId(blueprint.getId())).isEmpty();

    blueprintGroupLinkService.removeGroupLink(blueprint.getId(), group.getId());
    assertThat(blueprintGroupLinkRepository.findAllByBlueprintId(blueprint.getId())).isEmpty();
  }

  @Test
  void addGroupLinkRequiresExistingGroup() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Blueprint blueprint = createBlueprint("bp-group-missing", now);
    UUID missingGroupId = UUID.fromString("00000000-0000-0000-0000-000000000951");

    assertThatThrownBy(
            () -> blueprintGroupLinkService.addGroupLink(blueprint.getId(), missingGroupId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void removeGroupLinkRequiresExistingGroup() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Blueprint blueprint = createBlueprint("bp-group-remove-missing", now);
    UUID missingGroupId = UUID.fromString("00000000-0000-0000-0000-000000000952");

    assertThatThrownBy(
            () -> blueprintGroupLinkService.removeGroupLink(blueprint.getId(), missingGroupId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  private Blueprint createBlueprint(String name, OffsetDateTime now) {
    return blueprintRepository.save(
        new Blueprint(
            name,
            false,
            1,
            "ghcr.io/spookly/hytale:latest",
            null,
            "[\"./run.sh\"]",
            null,
            now,
            now));
  }

  private InstanceGroup createGroup(String name, OffsetDateTime now) {
    return instanceGroupRepository.save(new InstanceGroup(name, null, now, now));
  }
}
