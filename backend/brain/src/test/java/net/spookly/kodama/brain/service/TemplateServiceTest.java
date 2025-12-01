package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateTemplateRequest;
import net.spookly.kodama.brain.dto.CreateTemplateVersionRequest;
import net.spookly.kodama.brain.dto.TemplateDto;
import net.spookly.kodama.brain.dto.TemplateVersionDto;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TemplateService.class)
class TemplateServiceTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Test
    void createTemplatePersistsEntity() {
        TemplateDto templateDto = templateService.createTemplate(
                new CreateTemplateRequest("Base Template", "Base description", TemplateType.CUSTOM, CREATOR_ID)
        );

        Template persisted = templateRepository.findById(templateDto.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Base Template");
        assertThat(persisted.getDescription()).isEqualTo("Base description");
        assertThat(persisted.getCreatedBy()).isEqualTo(CREATOR_ID);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void createTemplateRejectsDuplicateNames() {
        CreateTemplateRequest request =
                new CreateTemplateRequest("Duplicate", "desc", TemplateType.CUSTOM, CREATOR_ID);
        templateService.createTemplate(request);

        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void addVersionPersistsTemplateVersion() {
        TemplateDto templateDto = templateService.createTemplate(
                new CreateTemplateRequest("TemplateWithVersion", "desc", TemplateType.CUSTOM, CREATOR_ID)
        );

        TemplateVersionDto versionDto = templateService.addVersion(
                templateDto.getId(),
                new CreateTemplateVersionRequest("1.0.0", "checksum", "s3/path", "{\"map\":\"alpha\"}")
        );

        TemplateVersion persisted = templateVersionRepository.findById(versionDto.getId()).orElseThrow();
        assertThat(persisted.getTemplate().getId()).isEqualTo(templateDto.getId());
        assertThat(persisted.getVersion()).isEqualTo("1.0.0");
        assertThat(persisted.getChecksum()).isEqualTo("checksum");
    }

    @Test
    void addVersionRejectsDuplicateVersionForTemplate() {
        TemplateDto templateDto = templateService.createTemplate(
                new CreateTemplateRequest("TemplateDuplicateVersion", "desc", TemplateType.CUSTOM, CREATOR_ID)
        );

        CreateTemplateVersionRequest versionRequest =
                new CreateTemplateVersionRequest("1.0.0", "checksum", "s3/path", null);
        templateService.addVersion(templateDto.getId(), versionRequest);

        assertThatThrownBy(() -> templateService.addVersion(templateDto.getId(), versionRequest))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void listVersionsRequiresExistingTemplate() {
        assertThatThrownBy(() -> templateService.listVersions(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
