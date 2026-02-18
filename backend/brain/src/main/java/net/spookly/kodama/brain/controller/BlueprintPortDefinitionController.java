package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionDto;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionRequest;
import net.spookly.kodama.brain.service.BlueprintPortDefinitionService;
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
@RequestMapping("/api/blueprints/{id}/ports")
public class BlueprintPortDefinitionController {

    private final BlueprintPortDefinitionService blueprintPortDefinitionService;

    public BlueprintPortDefinitionController(BlueprintPortDefinitionService blueprintPortDefinitionService) {
        this.blueprintPortDefinitionService = blueprintPortDefinitionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public List<BlueprintPortDefinitionDto> listPortDefinitions(@PathVariable("id") UUID blueprintId) {
        return blueprintPortDefinitionService.listPortDefinitions(blueprintId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public BlueprintPortDefinitionDto addPortDefinition(
            @PathVariable("id") UUID blueprintId,
            @Valid @RequestBody BlueprintPortDefinitionRequest request
    ) {
        return blueprintPortDefinitionService.addPortDefinition(blueprintId, request);
    }

    @DeleteMapping("/{portId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void removePortDefinition(@PathVariable("id") UUID blueprintId, @PathVariable UUID portId) {
        blueprintPortDefinitionService.removePortDefinition(blueprintId, portId);
    }
}
