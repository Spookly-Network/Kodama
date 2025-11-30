package net.spookly.kodama.brain.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import net.spookly.kodama.brain.domain.template.Template;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<@NonNull Template, @NonNull UUID> {

    Optional<Template> findByName(String name);
}
