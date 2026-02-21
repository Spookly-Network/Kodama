package net.spookly.kodama.brain.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.dto.CreateInstanceGroupRequest;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.service.InstanceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instance-groups")
public class InstanceGroupController {

  private final InstanceGroupService instanceGroupService;

  public InstanceGroupController(InstanceGroupService instanceGroupService) {
    this.instanceGroupService = instanceGroupService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public List<InstanceGroupDto> listGroups() {
    return instanceGroupService.listGroups();
  }

  @GetMapping("/{groupId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public InstanceGroupDto getGroup(@PathVariable UUID groupId) {
    return instanceGroupService.getGroup(groupId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public InstanceGroupDto createGroup(@Valid @RequestBody CreateInstanceGroupRequest request) {
    return instanceGroupService.createGroup(request);
  }
}
