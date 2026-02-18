package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.TemplateAssignmentDto;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
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
    private BlueprintRepository blueprintRepository;

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
    void addInstanceAssignmentPersistsAndListsAssignments() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Instance Template", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);
        Instance instance = createInstance("instance-list", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(templateVersion.getId());
        request.setPriority(2);

        TemplateAssignmentDto created = templateAssignmentService.addInstanceAssignment(instance.getId(), request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTemplateId()).isEqualTo(template.getId());
        assertThat(created.getTemplateVersionId()).isEqualTo(templateVersion.getId());
        assertThat(created.getPriority()).isEqualTo(2);

        List<TemplateAssignmentDto> assignments = templateAssignmentService.listInstanceAssignments(instance.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getId()).isEqualTo(created.getId());
    }

    @Test
    void removeInstanceAssignmentDeletesAssignment() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Instance Template Remove", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);
        Instance instance = createInstance("instance-remove", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest(
                template.getId(),
                templateVersion.getId(),
                1
        );
        TemplateAssignmentDto created = templateAssignmentService.addInstanceAssignment(instance.getId(), request);

        templateAssignmentService.removeInstanceAssignment(instance.getId(), created.getId());

        assertThat(templateAssignmentService.listInstanceAssignments(instance.getId())).isEmpty();
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
    void addGroupAssignmentPersistsAndListsAssignments() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Group Template", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);
        InstanceGroup group = createGroup("group-two", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(templateVersion.getId());

        TemplateAssignmentDto created = templateAssignmentService.addGroupAssignment(group.getId(), request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTemplateId()).isEqualTo(template.getId());
        assertThat(created.getTemplateVersionId()).isEqualTo(templateVersion.getId());
        assertThat(created.getPriority()).isZero();

        List<TemplateAssignmentDto> assignments = templateAssignmentService.listGroupAssignments(group.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getId()).isEqualTo(created.getId());
    }

    @Test
    void removeGroupAssignmentDeletesAssignment() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Group Template Remove", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);
        InstanceGroup group = createGroup("group-remove", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest(
                template.getId(),
                templateVersion.getId(),
                3
        );
        TemplateAssignmentDto created = templateAssignmentService.addGroupAssignment(group.getId(), request);

        templateAssignmentService.removeGroupAssignment(group.getId(), created.getId());

        assertThat(templateAssignmentService.listGroupAssignments(group.getId())).isEmpty();
    }

    @Test
    void removeGroupAssignmentRejectsAssignmentFromOtherGroup() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Group Template Invalid", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);
        InstanceGroup group = createGroup("group-three", now);
        InstanceGroup otherGroup = createGroup("group-four", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest(
                template.getId(),
                templateVersion.getId(),
                1
        );
        TemplateAssignmentDto created = templateAssignmentService.addGroupAssignment(group.getId(), request);

        assertThatThrownBy(() -> templateAssignmentService.removeGroupAssignment(otherGroup.getId(), created.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listGroupAssignmentsRequiresExistingGroup() {
        UUID missingGroupId = UUID.fromString("00000000-0000-0000-0000-000000000905");

        assertThatThrownBy(() -> templateAssignmentService.listGroupAssignments(missingGroupId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addGroupAssignmentRejectsMissingTemplate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        InstanceGroup group = createGroup("group-missing-template", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000910"));
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addGroupAssignment(group.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addGroupAssignmentRejectsMissingTemplateVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Template Missing Version", now);
        InstanceGroup group = createGroup("group-missing-version", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000911"));
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

    @Test
    void addBlueprintAssignmentPersistsAndListsAssignments() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("blueprint-assignments", now);
        Template template = createTemplate("Blueprint Template", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(templateVersion.getId());
        request.setPriority(4);

        TemplateAssignmentDto created = templateAssignmentService.addBlueprintAssignment(blueprint.getId(), request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTemplateId()).isEqualTo(template.getId());
        assertThat(created.getTemplateVersionId()).isEqualTo(templateVersion.getId());
        assertThat(created.getPriority()).isEqualTo(4);

        List<TemplateAssignmentDto> assignments = templateAssignmentService.listBlueprintAssignments(blueprint.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getId()).isEqualTo(created.getId());
    }

    @Test
    void removeBlueprintAssignmentDeletesAssignment() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("blueprint-remove", now);
        Template template = createTemplate("Blueprint Template Remove", now);
        TemplateVersion templateVersion = createTemplateVersion(template, "1.0.0", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest(
                template.getId(),
                templateVersion.getId(),
                1
        );
        TemplateAssignmentDto created = templateAssignmentService.addBlueprintAssignment(blueprint.getId(), request);

        templateAssignmentService.removeBlueprintAssignment(blueprint.getId(), created.getId());

        assertThat(templateAssignmentService.listBlueprintAssignments(blueprint.getId())).isEmpty();
    }

    @Test
    void addBlueprintAssignmentRejectsMissingTemplateVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("blueprint-missing-version", now);
        Template template = createTemplate("Blueprint Template Missing Version", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000912"));
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addBlueprintAssignment(blueprint.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addBlueprintAssignmentRejectsMissingTemplate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("blueprint-missing-template", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000913"));
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addBlueprintAssignment(blueprint.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addBlueprintAssignmentRejectsMismatchedTemplateVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("blueprint-mismatch", now);
        Template template = createTemplate("Blueprint Template A", now);
        Template otherTemplate = createTemplate("Blueprint Template B", now);
        TemplateVersion otherVersion = createTemplateVersion(otherTemplate, "1.0.0", now);

        TemplateAssignmentRequest request = new TemplateAssignmentRequest();
        request.setTemplateId(template.getId());
        request.setTemplateVersionId(otherVersion.getId());
        request.setPriority(0);

        assertThatThrownBy(() -> templateAssignmentService.addBlueprintAssignment(blueprint.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Template createTemplate(String name, OffsetDateTime now) {
        return templateRepository.save(new Template(name, "desc", TemplateType.CUSTOM, now, REQUESTER_USERNAME));
    }

    private Blueprint createBlueprint(String name, OffsetDateTime now) {
        return blueprintRepository.save(new Blueprint(
                name,
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                "[\"./run.sh\"]",
                null,
                now,
                now
        ));
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
