package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.service.BlueprintGroupLinkService;
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
@RequestMapping("/api/blueprints/{id}/groups")
public class BlueprintGroupLinkController {

  private final BlueprintGroupLinkService blueprintGroupLinkService;

  public BlueprintGroupLinkController(BlueprintGroupLinkService blueprintGroupLinkService) {
    this.blueprintGroupLinkService = blueprintGroupLinkService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public List<InstanceGroupDto> listGroups(@PathVariable("id") UUID blueprintId) {
    return blueprintGroupLinkService.listGroupLinks(blueprintId);
  }

  @PutMapping("/{groupId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public void addGroupLink(@PathVariable("id") UUID blueprintId, @PathVariable UUID groupId) {
    blueprintGroupLinkService.addGroupLink(blueprintId, groupId);
  }

  @DeleteMapping("/{groupId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public void removeGroupLink(@PathVariable("id") UUID blueprintId, @PathVariable UUID groupId) {
    blueprintGroupLinkService.removeGroupLink(blueprintId, groupId);
  }
}
