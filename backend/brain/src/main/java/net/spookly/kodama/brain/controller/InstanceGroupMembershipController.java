package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.service.InstanceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instances/{id}/groups")
public class InstanceGroupMembershipController {

    private final InstanceGroupService instanceGroupService;

    public InstanceGroupMembershipController(InstanceGroupService instanceGroupService) {
        this.instanceGroupService = instanceGroupService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public List<InstanceGroupDto> listGroups(@PathVariable("id") UUID instanceId) {
        return instanceGroupService.listGroupsForInstance(instanceId);
    }

    @PutMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void addMembership(@PathVariable("id") UUID instanceId, @PathVariable UUID groupId) {
        instanceGroupService.addMembership(instanceId, groupId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void removeMembership(@PathVariable("id") UUID instanceId, @PathVariable UUID groupId) {
        instanceGroupService.removeMembership(instanceId, groupId);
    }
}
