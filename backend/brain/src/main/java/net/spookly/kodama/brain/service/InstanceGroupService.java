package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembershipId;
import net.spookly.kodama.brain.dto.CreateInstanceGroupRequest;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InstanceGroupService {

    private final InstanceGroupRepository instanceGroupRepository;
    private final InstanceGroupMembershipRepository membershipRepository;
    private final InstanceRepository instanceRepository;

    public InstanceGroupService(
            InstanceGroupRepository instanceGroupRepository,
            InstanceGroupMembershipRepository membershipRepository,
            InstanceRepository instanceRepository
    ) {
        this.instanceGroupRepository = instanceGroupRepository;
        this.membershipRepository = membershipRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional(readOnly = true)
    public List<InstanceGroupDto> listGroups() {
        return instanceGroupRepository.findAll().stream()
                .map(InstanceGroupDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstanceGroupDto getGroup(UUID id) {
        InstanceGroup group = instanceGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        return InstanceGroupDto.fromEntity(group);
    }

    public InstanceGroupDto createGroup(CreateInstanceGroupRequest request) {
        instanceGroupRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group with the same name already exists");
        });
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        InstanceGroup group = new InstanceGroup(request.getName(), request.getDescription(), now, now);
        InstanceGroup saved = instanceGroupRepository.save(group);
        return InstanceGroupDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InstanceGroupDto> listGroupsForInstance(UUID instanceId) {
        ensureInstanceExists(instanceId);
        return membershipRepository.findAllByInstanceId(instanceId).stream()
                .map(InstanceGroupMembership::getGroup)
                .map(InstanceGroupDto::fromEntity)
                .toList();
    }

    public void addMembership(UUID instanceId, UUID groupId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
        InstanceGroup group = instanceGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        InstanceGroupMembershipId membershipId = new InstanceGroupMembershipId(instance.getId(), group.getId());
        if (membershipRepository.existsById(membershipId)) {
            return;
        }

        membershipRepository.save(new InstanceGroupMembership(instance, group));
    }

    public void removeMembership(UUID instanceId, UUID groupId) {
        ensureInstanceExists(instanceId);
        ensureGroupExists(groupId);
        InstanceGroupMembershipId membershipId = new InstanceGroupMembershipId(instanceId, groupId);
        if (membershipRepository.existsById(membershipId)) {
            membershipRepository.deleteById(membershipId);
        }
    }

    private void ensureInstanceExists(UUID instanceId) {
        if (!instanceRepository.existsById(instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found");
        }
    }

    private void ensureGroupExists(UUID groupId) {
        if (!instanceGroupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
    }
}
