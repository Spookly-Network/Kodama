package net.spookly.kodama.brain.service;

import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLinkId;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.repository.BlueprintGroupLinkRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BlueprintGroupLinkService {

    private final BlueprintRepository blueprintRepository;
    private final InstanceGroupRepository instanceGroupRepository;
    private final BlueprintGroupLinkRepository blueprintGroupLinkRepository;

    public BlueprintGroupLinkService(
            BlueprintRepository blueprintRepository,
            InstanceGroupRepository instanceGroupRepository,
            BlueprintGroupLinkRepository blueprintGroupLinkRepository
    ) {
        this.blueprintRepository = blueprintRepository;
        this.instanceGroupRepository = instanceGroupRepository;
        this.blueprintGroupLinkRepository = blueprintGroupLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<InstanceGroupDto> listGroupLinks(UUID blueprintId) {
        ensureBlueprintExists(blueprintId);
        return blueprintGroupLinkRepository.findAllByBlueprintId(blueprintId).stream()
                .map(BlueprintGroupLink::getGroup)
                .map(InstanceGroupDto::fromEntity)
                .toList();
    }

    public void addGroupLink(UUID blueprintId, UUID groupId) {
        Blueprint blueprint = loadBlueprint(blueprintId);
        InstanceGroup group = loadGroup(groupId);
        BlueprintGroupLinkId linkId = new BlueprintGroupLinkId(blueprint.getId(), group.getId());
        if (blueprintGroupLinkRepository.existsById(linkId)) {
            return;
        }
        blueprintGroupLinkRepository.save(new BlueprintGroupLink(blueprint, group));
    }

    public void removeGroupLink(UUID blueprintId, UUID groupId) {
        ensureBlueprintExists(blueprintId);
        ensureGroupExists(groupId);

        BlueprintGroupLinkId linkId = new BlueprintGroupLinkId(blueprintId, groupId);
        if (blueprintGroupLinkRepository.existsById(linkId)) {
            blueprintGroupLinkRepository.deleteById(linkId);
        }
    }

    private Blueprint loadBlueprint(UUID blueprintId) {
        return blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
    }

    private InstanceGroup loadGroup(UUID groupId) {
        return instanceGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private void ensureBlueprintExists(UUID blueprintId) {
        if (blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found");
        }
    }

    private void ensureGroupExists(UUID groupId) {
        if (!instanceGroupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
    }
}
