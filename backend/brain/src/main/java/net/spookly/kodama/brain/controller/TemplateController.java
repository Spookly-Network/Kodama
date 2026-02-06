package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import net.spookly.kodama.brain.dto.CreateTemplateRequest;
import net.spookly.kodama.brain.dto.CreateTemplateVersionRequest;
import net.spookly.kodama.brain.dto.TemplateDto;
import net.spookly.kodama.brain.dto.TemplateVersionDto;
import net.spookly.kodama.brain.security.UserPrincipal;
import net.spookly.kodama.brain.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    // Used when auth is disabled and no principal is available.
    private static final String SYSTEM_USERNAME = "system";

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public List<TemplateDto> listTemplates() {
        return templateService.listTemplates();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public TemplateDto getTemplate(@PathVariable UUID id) {
        return templateService.getTemplate(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TemplateDto createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String createdByUsername = resolveCreatedByUsername(principal);
        return templateService.createTemplate(request, createdByUsername);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TemplateVersionDto addVersion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateVersionRequest request
    ) {
        return templateService.addVersion(id, request);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public List<TemplateVersionDto> listVersions(@PathVariable UUID id) {
        return templateService.listVersions(id);
    }

    private String resolveCreatedByUsername(UserPrincipal principal) {
        if (principal == null || principal.getUsername() == null || principal.getUsername().isBlank()) {
            return SYSTEM_USERNAME;
        }
        return principal.getUsername();
    }
}
