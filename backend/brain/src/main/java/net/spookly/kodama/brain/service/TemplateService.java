package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.NonNull;
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

  private static final Comparator<TemplateVersion> LATEST_VERSION_ORDER =
      Comparator.comparing(TemplateVersion::getCreatedAt).thenComparing(TemplateVersion::getId);

  private final TemplateRepository templateRepository;
  private final TemplateVersionRepository templateVersionRepository;

  public TemplateService(
      TemplateRepository templateRepository, TemplateVersionRepository templateVersionRepository) {
    this.templateRepository = templateRepository;
    this.templateVersionRepository = templateVersionRepository;
  }

  @Transactional(readOnly = true)
  public List<TemplateDto> listTemplates() {
    return templateRepository.findAll().stream().map(TemplateDto::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public TemplateDto getTemplate(@NonNull UUID id) {
    Template template = getTemplateEntity(id);
    return TemplateDto.fromEntity(template);
  }

  public TemplateDto createTemplate(
      @NonNull CreateTemplateRequest request, @NonNull String createdByUsername) {
    templateRepository
        .findByName(request.getName())
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "Template with the same name already exists");
            });

    Template template =
        new Template(
            request.getName(),
            request.getDescription(),
            request.getType(),
            OffsetDateTime.now(ZoneOffset.UTC),
            createdByUsername);
    Template saved = templateRepository.save(template);
    return TemplateDto.fromEntity(saved);
  }

  public TemplateVersionDto addVersion(
      @NonNull UUID templateId, @NonNull CreateTemplateVersionRequest request) {
    Template template = getTemplateEntity(templateId);

    templateVersionRepository
        .findByTemplateAndVersion(template, request.getVersion())
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "Template version already exists");
            });

    TemplateVersion templateVersion =
        new TemplateVersion(
            template,
            request.getVersion(),
            request.getChecksum(),
            request.getS3Key(),
            request.getMetadataJson(),
            OffsetDateTime.now(ZoneOffset.UTC));

    TemplateVersion saved = templateVersionRepository.save(templateVersion);
    return TemplateVersionDto.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<TemplateVersionDto> listVersions(@NonNull UUID templateId) {
    Template template = getTemplateEntity(templateId);
    return templateVersionRepository.findAllByTemplateOrderByCreatedAtDesc(template).stream()
        .map(TemplateVersionDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<UUID, Template> loadTemplatesById(Collection<UUID> templateIds) {
    if (templateIds == null || templateIds.isEmpty()) {
      return Map.of();
    }

    Set<UUID> deduplicatedIds = new HashSet<>(templateIds);
    if (deduplicatedIds.contains(null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required");
    }

    Map<UUID, Template> templatesById =
        templateRepository.findAllById(deduplicatedIds).stream()
            .collect(Collectors.toMap(Template::getId, template -> template));

    if (templatesById.size() != deduplicatedIds.size()) {
      Set<UUID> missingIds = new HashSet<>(deduplicatedIds);
      missingIds.removeAll(templatesById.keySet());
      UUID missing = missingIds.stream().findFirst().orElse(null);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Template not found" + (missing == null ? "" : ": " + missing));
    }

    return templatesById;
  }

  @Transactional(readOnly = true)
  public Map<UUID, TemplateVersion> loadTemplateVersionsById(Collection<UUID> templateVersionIds) {
    if (templateVersionIds == null || templateVersionIds.isEmpty()) {
      return Map.of();
    }

    Set<UUID> deduplicatedIds = new HashSet<>(templateVersionIds);
    if (deduplicatedIds.contains(null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateVersionId is required");
    }

    Map<UUID, TemplateVersion> versionsById =
        templateVersionRepository.findAllById(deduplicatedIds).stream()
            .collect(Collectors.toMap(TemplateVersion::getId, version -> version));

    if (versionsById.size() != deduplicatedIds.size()) {
      Set<UUID> missingIds = new HashSet<>(deduplicatedIds);
      missingIds.removeAll(versionsById.keySet());
      UUID missing = missingIds.stream().findFirst().orElse(null);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Template version not found" + (missing == null ? "" : ": " + missing));
    }

    return versionsById;
  }

  @Transactional(readOnly = true)
  public Map<UUID, TemplateVersion> loadLatestTemplateVersionsByTemplateIds(
      Collection<UUID> templateIds) {
    if (templateIds == null || templateIds.isEmpty()) {
      return Map.of();
    }

    Set<UUID> deduplicatedIds = new HashSet<>(templateIds);
    if (deduplicatedIds.contains(null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required");
    }

    loadTemplatesById(deduplicatedIds);

    Map<UUID, TemplateVersion> latestVersions =
        selectLatestVersions(templateVersionRepository.findLatestForTemplateIds(deduplicatedIds));

    if (latestVersions.size() != deduplicatedIds.size()) {
      Set<UUID> missingIds = new HashSet<>(deduplicatedIds);
      missingIds.removeAll(latestVersions.keySet());
      UUID missing = missingIds.stream().findFirst().orElse(null);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Template has no versions" + (missing == null ? "" : ": " + missing));
    }

    return latestVersions;
  }

  @Transactional(readOnly = true)
  public void ensureTemplatesHaveVersions(Collection<UUID> templateIdsWithoutVersion) {
    if (templateIdsWithoutVersion == null || templateIdsWithoutVersion.isEmpty()) {
      return;
    }
    loadLatestTemplateVersionsByTemplateIds(templateIdsWithoutVersion);
  }

  private Template getTemplateEntity(@NonNull UUID id) {
    return templateRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
  }

  private Map<UUID, TemplateVersion> selectLatestVersions(List<TemplateVersion> versions) {
    Map<UUID, TemplateVersion> latestByTemplate = new HashMap<>();
    for (TemplateVersion version : versions) {
      UUID templateId = version.getTemplate().getId();
      TemplateVersion current = latestByTemplate.get(templateId);
      if (current == null || LATEST_VERSION_ORDER.compare(version, current) > 0) {
        latestByTemplate.put(templateId, version);
      }
    }
    return latestByTemplate;
  }
}
