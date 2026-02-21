package net.spookly.kodama.brain.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLinkId;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.repository.BlueprintGroupLinkRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BlueprintGroupLinkService {

  private static final String BLUEPRINT_NOT_FOUND_MESSAGE = "Blueprint not found";
  private static final String GROUP_NOT_FOUND_MESSAGE = "Group not found";
  private static final String GROUP_LINK_PRIMARY_KEY_CONSTRAINT = "pk_blueprint_group_links";
  private static final String GROUP_LINK_PRIMARY_INDEX = "blueprint_group_links.primary";

  private final BlueprintRepository blueprintRepository;
  private final InstanceGroupRepository instanceGroupRepository;
  private final BlueprintGroupLinkRepository blueprintGroupLinkRepository;

  public BlueprintGroupLinkService(
      BlueprintRepository blueprintRepository,
      InstanceGroupRepository instanceGroupRepository,
      BlueprintGroupLinkRepository blueprintGroupLinkRepository) {
    this.blueprintRepository = blueprintRepository;
    this.instanceGroupRepository = instanceGroupRepository;
    this.blueprintGroupLinkRepository = blueprintGroupLinkRepository;
  }

  @Transactional(readOnly = true)
  public List<InstanceGroupDto> listGroupLinks(UUID blueprintId) {
    loadBlueprint(blueprintId);
    return blueprintGroupLinkRepository.findAllByBlueprintId(blueprintId).stream()
        .map(BlueprintGroupLink::getGroup)
        .map(InstanceGroupDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<UUID> listLinkedGroupIds(UUID blueprintId) {
    loadBlueprint(blueprintId);
    return blueprintGroupLinkRepository.findAllByBlueprintId(blueprintId).stream()
        .map(link -> link.getGroup().getId())
        .toList();
  }

  public void addGroupLink(UUID blueprintId, UUID groupId) {
    Blueprint blueprint = loadBlueprint(blueprintId);
    InstanceGroup group = loadGroup(groupId);
    BlueprintGroupLinkId linkId = new BlueprintGroupLinkId(blueprint.getId(), group.getId());
    if (blueprintGroupLinkRepository.existsById(linkId)) {
      return;
    }
    try {
      blueprintGroupLinkRepository.saveAndFlush(new BlueprintGroupLink(blueprint, group));
    } catch (DataIntegrityViolationException ex) {
      if (isDuplicateGroupLinkViolation(ex)) {
        return;
      }
      throw ex;
    }
  }

  public void removeGroupLink(UUID blueprintId, UUID groupId) {
    loadBlueprint(blueprintId);
    loadGroup(groupId);

    BlueprintGroupLinkId linkId = new BlueprintGroupLinkId(blueprintId, groupId);
    if (blueprintGroupLinkRepository.existsById(linkId)) {
      blueprintGroupLinkRepository.deleteById(linkId);
    }
  }

  private Blueprint loadBlueprint(UUID blueprintId) {
    return blueprintRepository
        .findByIdAndDeletedAtIsNull(blueprintId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, BLUEPRINT_NOT_FOUND_MESSAGE));
  }

  private InstanceGroup loadGroup(UUID groupId) {
    return instanceGroupRepository
        .findById(groupId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));
  }

  private boolean isDuplicateGroupLinkViolation(Throwable ex) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (current instanceof ConstraintViolationException constraintViolationException) {
        String constraintName = constraintViolationException.getConstraintName();
        if (constraintName != null
            && constraintName
                .toLowerCase(Locale.ROOT)
                .contains(GROUP_LINK_PRIMARY_KEY_CONSTRAINT)) {
          return true;
        }
      }

      String message = current.getMessage();
      if (message != null) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains(GROUP_LINK_PRIMARY_KEY_CONSTRAINT)
            || normalizedMessage.contains(GROUP_LINK_PRIMARY_INDEX)
            || (normalizedMessage.contains("duplicate entry")
                && normalizedMessage.contains("primary"))) {
          return true;
        }
      }
    }
    return false;
  }
}
