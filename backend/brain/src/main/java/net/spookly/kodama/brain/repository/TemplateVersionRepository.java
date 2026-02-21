package net.spookly.kodama.brain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateVersionRepository
    extends JpaRepository<@NonNull TemplateVersion, @NonNull UUID> {

  Optional<TemplateVersion> findByTemplateAndVersion(Template template, String version);

  List<TemplateVersion> findAllByTemplateOrderByCreatedAtDesc(Template template);

  Optional<TemplateVersion> findFirstByTemplateOrderByCreatedAtDesc(Template template);

  @Query(
      """
            select version
            from TemplateVersion version
            where version.template.id in :templateIds
              and version.createdAt = (
                select max(innerVersion.createdAt)
                from TemplateVersion innerVersion
                where innerVersion.template.id = version.template.id
              )
            """)
  List<TemplateVersion> findLatestForTemplateIds(
      @Param("templateIds") Collection<UUID> templateIds);
}
