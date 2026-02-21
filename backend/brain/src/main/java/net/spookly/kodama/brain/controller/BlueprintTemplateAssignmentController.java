package net.spookly.kodama.brain.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.dto.TemplateAssignmentDto;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.service.TemplateAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blueprints/{id}/template-assignments")
public class BlueprintTemplateAssignmentController {

  private final TemplateAssignmentService templateAssignmentService;

  public BlueprintTemplateAssignmentController(
      TemplateAssignmentService templateAssignmentService) {
    this.templateAssignmentService = templateAssignmentService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public List<TemplateAssignmentDto> listAssignments(@PathVariable("id") UUID blueprintId) {
    return templateAssignmentService.listBlueprintAssignments(blueprintId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public TemplateAssignmentDto addAssignment(
      @PathVariable("id") UUID blueprintId, @Valid @RequestBody TemplateAssignmentRequest request) {
    return templateAssignmentService.addBlueprintAssignment(blueprintId, request);
  }

  @DeleteMapping("/{assignmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public void removeAssignment(
      @PathVariable("id") UUID blueprintId, @PathVariable UUID assignmentId) {
    templateAssignmentService.removeBlueprintAssignment(blueprintId, assignmentId);
  }
}
