package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;
import java.util.UUID;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLinkId;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.repository.BlueprintGroupLinkRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BlueprintGroupLinkServiceWriteConflictTest {

  @Mock private BlueprintRepository blueprintRepository;

  @Mock private InstanceGroupRepository instanceGroupRepository;

  @Mock private BlueprintGroupLinkRepository blueprintGroupLinkRepository;

  private BlueprintGroupLinkService blueprintGroupLinkService;

  @BeforeEach
  void setUp() {
    blueprintGroupLinkService =
        new BlueprintGroupLinkService(
            blueprintRepository, instanceGroupRepository, blueprintGroupLinkRepository);
  }

  @Test
  void addGroupLinkTreatsDuplicateKeyViolationAsIdempotentSuccess() {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001581");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001582");
    Blueprint blueprint = blueprintWithId(blueprintId);
    InstanceGroup group = groupWithId(groupId);

    when(blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId))
        .thenReturn(Optional.of(blueprint));
    when(instanceGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(blueprintGroupLinkRepository.existsById(new BlueprintGroupLinkId(blueprintId, groupId)))
        .thenReturn(false);
    when(blueprintGroupLinkRepository.saveAndFlush(any(BlueprintGroupLink.class)))
        .thenThrow(duplicateLinkViolation());

    assertThatCode(() -> blueprintGroupLinkService.addGroupLink(blueprintId, groupId))
        .doesNotThrowAnyException();
  }

  @Test
  void addGroupLinkPropagatesNonDuplicateIntegrityViolation() {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001583");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001584");
    Blueprint blueprint = blueprintWithId(blueprintId);
    InstanceGroup group = groupWithId(groupId);
    DataIntegrityViolationException failure =
        new DataIntegrityViolationException(
            "write failed",
            new ConstraintViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                    "Duplicate entry for key 'blueprint_group_links.some_other_constraint'"),
                "some_other_constraint"));

    when(blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId))
        .thenReturn(Optional.of(blueprint));
    when(instanceGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(blueprintGroupLinkRepository.existsById(new BlueprintGroupLinkId(blueprintId, groupId)))
        .thenReturn(false);
    when(blueprintGroupLinkRepository.saveAndFlush(any(BlueprintGroupLink.class)))
        .thenThrow(failure);

    assertThatThrownBy(() -> blueprintGroupLinkService.addGroupLink(blueprintId, groupId))
        .isSameAs(failure);
  }

  private Blueprint blueprintWithId(UUID id) {
    Blueprint blueprint = org.mockito.Mockito.mock(Blueprint.class);
    when(blueprint.getId()).thenReturn(id);
    return blueprint;
  }

  private InstanceGroup groupWithId(UUID id) {
    InstanceGroup group = org.mockito.Mockito.mock(InstanceGroup.class);
    when(group.getId()).thenReturn(id);
    return group;
  }

  private DataIntegrityViolationException duplicateLinkViolation() {
    return new DataIntegrityViolationException(
        "write failed",
        new ConstraintViolationException(
            "could not execute statement",
            new SQLIntegrityConstraintViolationException(
                "Duplicate entry for key 'blueprint_group_links.PRIMARY'"),
            "PRIMARY"));
  }
}
