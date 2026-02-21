package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.dto.CreateInstanceGroupRequest;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
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
@Import(InstanceGroupService.class)
class InstanceGroupServiceTest {

  @Container private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
  }

  @Autowired private InstanceGroupService instanceGroupService;

  @Autowired private InstanceGroupRepository instanceGroupRepository;

  @Autowired private InstanceGroupMembershipRepository membershipRepository;

  @Autowired private InstanceRepository instanceRepository;

  @Test
  void createGroupPersistsAndListsGroups() {
    InstanceGroupDto created =
        instanceGroupService.createGroup(
            new CreateInstanceGroupRequest("group-one", "Primary group"));

    assertThat(created.getId()).isNotNull();
    assertThat(created.getName()).isEqualTo("group-one");

    List<InstanceGroupDto> groups = instanceGroupService.listGroups();
    assertThat(groups).extracting(InstanceGroupDto::getId).contains(created.getId());

    InstanceGroupDto fetched = instanceGroupService.getGroup(created.getId());
    assertThat(fetched.getName()).isEqualTo("group-one");
    assertThat(fetched.getDescription()).isEqualTo("Primary group");
  }

  @Test
  void createGroupRejectsDuplicateNames() {
    instanceGroupService.createGroup(new CreateInstanceGroupRequest("duplicate", null));

    assertThatThrownBy(
            () ->
                instanceGroupService.createGroup(new CreateInstanceGroupRequest("duplicate", null)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void listGroupsForInstanceRequiresExistingInstance() {
    UUID missingInstanceId = UUID.fromString("00000000-0000-0000-0000-000000000901");

    assertThatThrownBy(() -> instanceGroupService.listGroupsForInstance(missingInstanceId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void addMembershipIsIdempotentAndListsGroups() {
    Instance instance = instanceRepository.save(createInstance("instance-one"));
    InstanceGroup group = instanceGroupRepository.save(createGroup("group-alpha"));

    instanceGroupService.addMembership(instance.getId(), group.getId());
    instanceGroupService.addMembership(instance.getId(), group.getId());

    List<InstanceGroupMembership> memberships =
        membershipRepository.findAllByInstanceId(instance.getId());
    assertThat(memberships).hasSize(1);
    assertThat(memberships.getFirst().getGroup().getId()).isEqualTo(group.getId());

    List<InstanceGroupDto> groups = instanceGroupService.listGroupsForInstance(instance.getId());
    assertThat(groups).extracting(InstanceGroupDto::getId).containsExactly(group.getId());
  }

  @Test
  void removeMembershipIsIdempotent() {
    Instance instance = instanceRepository.save(createInstance("instance-two"));
    InstanceGroup group = instanceGroupRepository.save(createGroup("group-beta"));

    instanceGroupService.addMembership(instance.getId(), group.getId());
    instanceGroupService.removeMembership(instance.getId(), group.getId());

    assertThat(membershipRepository.findAllByInstanceId(instance.getId())).isEmpty();

    instanceGroupService.removeMembership(instance.getId(), group.getId());
    assertThat(membershipRepository.findAllByInstanceId(instance.getId())).isEmpty();
  }

  @Test
  void addMembershipRequiresInstance() {
    InstanceGroup group = instanceGroupRepository.save(createGroup("group-missing-instance"));
    UUID missingInstanceId = UUID.fromString("00000000-0000-0000-0000-000000000902");

    assertThatThrownBy(() -> instanceGroupService.addMembership(missingInstanceId, group.getId()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void addMembershipRequiresGroup() {
    Instance instance = instanceRepository.save(createInstance("instance-three"));
    UUID missingGroupId = UUID.fromString("00000000-0000-0000-0000-000000000903");

    assertThatThrownBy(() -> instanceGroupService.addMembership(instance.getId(), missingGroupId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void removeMembershipRequiresGroup() {
    Instance instance = instanceRepository.save(createInstance("instance-four"));
    UUID missingGroupId = UUID.fromString("00000000-0000-0000-0000-000000000904");

    assertThatThrownBy(
            () -> instanceGroupService.removeMembership(instance.getId(), missingGroupId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  private Instance createInstance(String name) {
    OffsetDateTime now = OffsetDateTime.of(2025, 1, 5, 12, 0, 0, 0, ZoneOffset.UTC);
    return new Instance(
        name,
        "Display " + name,
        InstanceState.REQUESTED,
        null,
        null,
        "eu-west-1",
        null,
        Boolean.FALSE,
        null,
        null,
        now,
        now);
  }

  private InstanceGroup createGroup(String name) {
    OffsetDateTime now = OffsetDateTime.of(2025, 1, 5, 12, 0, 0, 0, ZoneOffset.UTC);
    return new InstanceGroup(name, null, now, now);
  }
}
