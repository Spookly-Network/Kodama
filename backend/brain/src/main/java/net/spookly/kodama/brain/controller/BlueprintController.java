package net.spookly.kodama.brain.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.dto.BlueprintDto;
import net.spookly.kodama.brain.dto.CreateBlueprintRequest;
import net.spookly.kodama.brain.dto.UpdateBlueprintRequest;
import net.spookly.kodama.brain.service.BlueprintService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blueprints")
public class BlueprintController {

  private final BlueprintService blueprintService;

  public BlueprintController(BlueprintService blueprintService) {
    this.blueprintService = blueprintService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public List<BlueprintDto> listBlueprints() {
    return blueprintService.listBlueprints();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
  public BlueprintDto getBlueprint(@PathVariable UUID id) {
    return blueprintService.getBlueprint(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public BlueprintDto createBlueprint(@Valid @RequestBody CreateBlueprintRequest request) {
    return blueprintService.createBlueprint(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public BlueprintDto updateBlueprint(
      @PathVariable UUID id, @Valid @RequestBody UpdateBlueprintRequest request) {
    return blueprintService.updateBlueprint(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
  public void deleteBlueprint(@PathVariable UUID id) {
    blueprintService.deleteBlueprint(id);
  }
}
