package net.spookly.kodama.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TemplateRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Test
    void saveAndLoadTemplateWithVersion() {
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        Template template =
                new Template("Base Hytale", "Base server template", TemplateType.CUSTOM, createdAt, UUID.randomUUID());
        Template savedTemplate = templateRepository.save(template);

        TemplateVersion version = new TemplateVersion(
                savedTemplate, "1.0.0", "checksum123", "templates/base/1.0.0.tar", "{\"map\":\"alpha\"}", createdAt);
        TemplateVersion savedVersion = templateVersionRepository.save(version);

        Template persistedTemplate = templateRepository.findById(savedTemplate.getId()).orElseThrow();
        TemplateVersion persistedVersion = templateVersionRepository.findById(savedVersion.getId()).orElseThrow();

        assertThat(persistedTemplate.getName()).isEqualTo("Base Hytale");
        assertThat(persistedTemplate.getType()).isEqualTo(TemplateType.CUSTOM);
        assertThat(persistedVersion.getTemplate().getId()).isEqualTo(savedTemplate.getId());
        assertThat(persistedVersion.getVersion()).isEqualTo("1.0.0");
        assertThat(persistedVersion.getChecksum()).isEqualTo("checksum123");
        assertThat(persistedVersion.getS3Key()).isEqualTo("templates/base/1.0.0.tar");
        assertThat(persistedVersion.getMetadataJson()).contains("alpha");
    }
}
