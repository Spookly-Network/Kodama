package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
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
@Import(TemplateAssignmentService.class)
class TemplateAssignmentServiceTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    private static final String REQUESTER_USERNAME = "admin";
    private static final UUID REQUESTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private TemplateAssignmentService templateAssignmentService;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Test
    void addInstanceAssignmentRejectsTemplateWithoutVersions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Template Without Versions", now);
        Instance instance = createInstance("instance-one", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addInstanceAssignment(instance.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addGroupAssignmentRejectsTemplateWithoutVersions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Group Template Without Versions", now);
        InstanceGroup group = createGroup("group-one", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addGroupAssignment(group.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addInstanceAssignmentRejectsMismatchedTemplateVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Template A", now);
        Template otherTemplate = createTemplate("Template B", now);
        TemplateVersion otherVersion = createTemplateVersion(otherTemplate, "1.0.0", now);
        Instance instance = createInstance("instance-mismatch", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(otherVersion.getId());
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addInstanceAssignment(instance.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Template createTemplate(String name, OffsetDateTime now) {
        return templateRepository.save(new Template(name, "desc", TemplateType.CUSTOM, now, REQUESTER_USERNAME));
    }

    private Instance createInstance(String name, OffsetDateTime now) {
        Instance instance = new Instance(
                name,
                name,
                InstanceState.REQUESTED,
                REQUESTER_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
        return instanceRepository.save(instance);
    }

    private InstanceGroup createGroup(String name, OffsetDateTime now) {
        InstanceGroup group = new InstanceGroup(name, null, now, now);
        return instanceGroupRepository.save(group);
    }

    private TemplateVersion createTemplateVersion(Template template, String version, OffsetDateTime now) {
        TemplateVersion templateVersion = new TemplateVersion(
                template,
                version,
                "checksum",
                "s3/key",
                null,
                now
        );
        return templateVersionRepository.save(templateVersion);
    }
}
