package net.spookly.kodama.brain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<@NonNull TemplateVersion, @NonNull UUID> {

    Optional<TemplateVersion> findByTemplateAndVersion(Template template, String version);

    List<TemplateVersion> findAllByTemplateOrderByCreatedAtDesc(Template template);
}
