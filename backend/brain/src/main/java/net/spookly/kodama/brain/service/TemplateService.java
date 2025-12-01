package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateTemplateRequest;
import net.spookly.kodama.brain.dto.CreateTemplateVersionRequest;
import net.spookly.kodama.brain.dto.TemplateDto;
import net.spookly.kodama.brain.dto.TemplateVersionDto;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public TemplateService(TemplateRepository templateRepository, TemplateVersionRepository templateVersionRepository) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateDto> listTemplates() {
        return templateRepository.findAll()
                .stream()
                .map(TemplateDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateDto getTemplate(UUID id) {
        Template template = getTemplateEntity(id);
        return TemplateDto.fromEntity(template);
    }

    public TemplateDto createTemplate(CreateTemplateRequest request) {
        templateRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template with the same name already exists");
        });

        Template template = new Template(
                request.getName(),
                request.getDescription(),
                request.getType(),
                OffsetDateTime.now(ZoneOffset.UTC),
                request.getCreatedBy()
        );
        Template saved = templateRepository.save(template);
        return TemplateDto.fromEntity(saved);
    }

    public TemplateVersionDto addVersion(UUID templateId, CreateTemplateVersionRequest request) {
        Template template = getTemplateEntity(templateId);

        templateVersionRepository.findByTemplateAndVersion(template, request.getVersion()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template version already exists");
        });

        TemplateVersion templateVersion = new TemplateVersion(
                template,
                request.getVersion(),
                request.getChecksum(),
                request.getS3Key(),
                request.getMetadataJson(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        TemplateVersion saved = templateVersionRepository.save(templateVersion);
        return TemplateVersionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TemplateVersionDto> listVersions(UUID templateId) {
        Template template = getTemplateEntity(templateId);
        return templateVersionRepository.findAllByTemplateOrderByCreatedAtDesc(template)
                .stream()
                .map(TemplateVersionDto::fromEntity)
                .toList();
    }

    private Template getTemplateEntity(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    }
}
