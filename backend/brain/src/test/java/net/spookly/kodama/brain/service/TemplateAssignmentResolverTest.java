package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.GroupTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.TemplateAssignmentSource;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.repository.GroupTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
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
@Import({TemplateAssignmentResolver.class, TemplateService.class})
class TemplateAssignmentResolverTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    private static final String CREATOR_USERNAME = "admin";

    @Autowired
    private TemplateAssignmentResolver resolver;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private InstanceGroupMembershipRepository membershipRepository;

    @Autowired
    private InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;

    @Autowired
    private GroupTemplateAssignmentRepository groupTemplateAssignmentRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Test
    void resolvePrefersInstanceAssignmentOverGroup() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Shared Template", now);
        TemplateVersion groupVersion = createTemplateVersion(template, "1.0.0", now.minusMinutes(5));
        TemplateVersion instanceVersion = createTemplateVersion(template, "2.0.0", now);

        Instance instance = createInstance("instance-one", now);
        InstanceGroup group = createGroup("group-one", now);
        membershipRepository.save(new InstanceGroupMembership(instance, group));

        groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                template,
                groupVersion,
                0
        ));
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                instanceVersion,
                5
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        ResolvedTemplateLayer layer = resolved.getFirst();
        assertThat(layer.templateVersion().getId()).isEqualTo(instanceVersion.getId());
        assertThat(layer.source()).isEqualTo(TemplateAssignmentSource.INSTANCE);
        assertThat(layer.priority()).isEqualTo(5);
    }

    @Test
    void resolveOrdersLayersByPriorityAcrossSources() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-two", now);
        InstanceGroup group = createGroup("group-two", now);
        membershipRepository.save(new InstanceGroupMembership(instance, group));

        Template templateA = createTemplate("Template A", now);
        Template templateB = createTemplate("Template B", now);
        Template templateC = createTemplate("Template C", now);
        TemplateVersion versionA = createTemplateVersion(templateA, "1.0.0", now.minusMinutes(3));
        TemplateVersion versionB = createTemplateVersion(templateB, "1.0.0", now.minusMinutes(2));
        TemplateVersion versionC = createTemplateVersion(templateC, "1.0.0", now.minusMinutes(1));

        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                templateA,
                versionA,
                5
        ));
        groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                templateB,
                versionB,
                1
        ));
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                templateC,
                versionC,
                0
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(3);
        assertThat(resolved.get(0).templateVersion().getId()).isEqualTo(versionC.getId());
        assertThat(resolved.get(0).orderIndex()).isZero();
        assertThat(resolved.get(1).templateVersion().getId()).isEqualTo(versionB.getId());
        assertThat(resolved.get(1).orderIndex()).isEqualTo(1);
        assertThat(resolved.get(2).templateVersion().getId()).isEqualTo(versionA.getId());
        assertThat(resolved.get(2).orderIndex()).isEqualTo(2);
    }

    @Test
    void resolvePreservesMultipleInstanceAssignmentsForSameTemplate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-three", now);
        InstanceGroup group = createGroup("group-three", now);
        membershipRepository.save(new InstanceGroupMembership(instance, group));

        Template template = createTemplate("Repeated Template", now);
        TemplateVersion versionA = createTemplateVersion(template, "1.0.0", now.minusMinutes(2));
        TemplateVersion versionB = createTemplateVersion(template, "2.0.0", now.minusMinutes(1));

        groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                template,
                versionA,
                0
        ));
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                versionA,
                2
        ));
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                versionB,
                5
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get(0).templateVersion().getId()).isEqualTo(versionA.getId());
        assertThat(resolved.get(0).priority()).isEqualTo(2);
        assertThat(resolved.get(0).source()).isEqualTo(TemplateAssignmentSource.INSTANCE);
        assertThat(resolved.get(1).templateVersion().getId()).isEqualTo(versionB.getId());
        assertThat(resolved.get(1).priority()).isEqualTo(5);
        assertThat(resolved.get(1).source()).isEqualTo(TemplateAssignmentSource.INSTANCE);
    }

    @Test
    void resolveUsesLatestVersionWhenAssignmentOmitsVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Latest Template", now);
        TemplateVersion older = createTemplateVersion(template, "1.0.0", now.minusMinutes(10));
        TemplateVersion newer = createTemplateVersion(template, "1.1.0", now);

        Instance instance = createInstance("instance-latest", now);
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                null,
                0
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        ResolvedTemplateLayer layer = resolved.getFirst();
        assertThat(layer.templateVersion().getId()).isEqualTo(newer.getId());
        assertThat(newer.getCreatedAt()).isAfter(older.getCreatedAt());
    }

    @Test
    void resolveDedupesGroupAssignmentsByGroupIdOnPriorityTie() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("Group Dedup Template", now);
        TemplateVersion version = createTemplateVersion(template, "1.0.0", now);

        Instance instance = createInstance("instance-group-dedup", now);
        InstanceGroup groupA = createGroup("group-a", now);
        InstanceGroup groupB = createGroup("group-b", now);
        membershipRepository.save(new InstanceGroupMembership(instance, groupA));
        membershipRepository.save(new InstanceGroupMembership(instance, groupB));

        GroupTemplateAssignment assignmentA = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                groupA,
                template,
                version,
                0
        ));
        GroupTemplateAssignment assignmentB = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                groupB,
                template,
                version,
                0
        ));

        GroupTemplateAssignment expected = groupA.getId().compareTo(groupB.getId()) < 0 ? assignmentA : assignmentB;

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        ResolvedTemplateLayer layer = resolved.getFirst();
        assertThat(layer.assignmentId()).isEqualTo(expected.getId());
        assertThat(layer.source()).isEqualTo(TemplateAssignmentSource.GROUP);
    }

    @Test
    void resolveOrdersSamePriorityByTemplateIdWithinSource() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-template-order", now);

        Template templateA = createTemplate("Template Order A", now);
        Template templateB = createTemplate("Template Order B", now);
        TemplateVersion versionA = createTemplateVersion(templateA, "1.0.0", now.minusMinutes(2));
        TemplateVersion versionB = createTemplateVersion(templateB, "1.0.0", now.minusMinutes(1));

        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                templateA,
                versionA,
                1
        ));
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                templateB,
                versionB,
                1
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(2);
        UUID expectedFirst = templateA.getId().compareTo(templateB.getId()) < 0
                ? templateA.getId()
                : templateB.getId();
        UUID expectedSecond = expectedFirst.equals(templateA.getId())
                ? templateB.getId()
                : templateA.getId();

        assertThat(resolved.get(0).templateId()).isEqualTo(expectedFirst);
        assertThat(resolved.get(1).templateId()).isEqualTo(expectedSecond);
        assertThat(resolved.get(0).source()).isEqualTo(TemplateAssignmentSource.INSTANCE);
        assertThat(resolved.get(1).source()).isEqualTo(TemplateAssignmentSource.INSTANCE);
    }

    @Test
    void resolveRejectsTemplateWithoutVersionsWhenAssignmentOmitsVersion() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("No Versions Template", now);
        Instance instance = createInstance("instance-no-versions", now);

        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                null,
                0
        ));

        assertThatThrownBy(() -> resolver.resolveForInstance(instance.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusException = (ResponseStatusException) ex;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void resolveSelectsDeterministicLatestVersionWhenTimestampsMatch() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime createdAt = now.minusMinutes(4);
        Template template = createTemplate("Latest Tie Template", now);
        TemplateVersion versionA = createTemplateVersion(template, "1.0.0", createdAt);
        TemplateVersion versionB = createTemplateVersion(template, "1.0.1", createdAt);

        Instance instance = createInstance("instance-latest-tie", now);
        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                null,
                0
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        TemplateVersion expected = versionA.getId().compareTo(versionB.getId()) >= 0 ? versionA : versionB;
        assertThat(resolved.getFirst().templateVersion().getId()).isEqualTo(expected.getId());
    }

    @Test
    void resolveDeduplicatesGroupAssignmentsByPriorityThenGroupId() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-group-dedup", now);
        InstanceGroup groupA = createGroup("group-dedup-a", now);
        InstanceGroup groupB = createGroup("group-dedup-b", now);
        membershipRepository.save(new InstanceGroupMembership(instance, groupA));
        membershipRepository.save(new InstanceGroupMembership(instance, groupB));

        Template template = createTemplate("Group Dedup Template", now);
        TemplateVersion version = createTemplateVersion(template, "1.0.0", now.minusMinutes(1));

        GroupTemplateAssignment assignmentA = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                groupA,
                template,
                version,
                0
        ));
        GroupTemplateAssignment assignmentB = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                groupB,
                template,
                version,
                0
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        UUID expectedGroupId = groupA.getId().compareTo(groupB.getId()) <= 0 ? groupA.getId() : groupB.getId();
        UUID expectedAssignmentId = expectedGroupId.equals(groupA.getId()) ? assignmentA.getId() : assignmentB.getId();
        assertThat(resolved.getFirst().assignmentId()).isEqualTo(expectedAssignmentId);
        assertThat(resolved.getFirst().source()).isEqualTo(TemplateAssignmentSource.GROUP);
    }

    @Test
    void resolveDeduplicatesGroupAssignmentsByAssignmentIdWhenGroupMatches() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-group-same", now);
        InstanceGroup group = createGroup("group-same", now);
        membershipRepository.save(new InstanceGroupMembership(instance, group));

        Template template = createTemplate("Group Same Template", now);
        TemplateVersion version = createTemplateVersion(template, "1.0.0", now.minusMinutes(1));

        GroupTemplateAssignment assignmentA = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                template,
                version,
                0
        ));
        GroupTemplateAssignment assignmentB = groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                template,
                version,
                0
        ));

        List<ResolvedTemplateLayer> resolved = resolver.resolveForInstance(instance.getId());

        assertThat(resolved).hasSize(1);
        UUID expectedAssignmentId = assignmentA.getId().compareTo(assignmentB.getId()) <= 0
                ? assignmentA.getId()
                : assignmentB.getId();
        assertThat(resolved.getFirst().assignmentId()).isEqualTo(expectedAssignmentId);
    }

    @Test
    void resolveIsStableAcrossRepeatedRuns() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = createInstance("instance-stable", now);
        InstanceGroup group = createGroup("group-stable", now);
        membershipRepository.save(new InstanceGroupMembership(instance, group));

        Template templateA = createTemplate("Stable Template A", now);
        Template templateB = createTemplate("Stable Template B", now);
        TemplateVersion versionA = createTemplateVersion(templateA, "1.0.0", now.minusMinutes(3));
        TemplateVersion versionB = createTemplateVersion(templateB, "1.0.0", now.minusMinutes(2));

        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                templateA,
                versionA,
                1
        ));
        groupTemplateAssignmentRepository.save(new GroupTemplateAssignment(
                group,
                templateB,
                versionB,
                1
        ));

        List<ResolvedTemplateLayer> first = resolver.resolveForInstance(instance.getId());
        List<ResolvedTemplateLayer> second = resolver.resolveForInstance(instance.getId());

        assertThat(second).hasSameSizeAs(first);
        assertThat(second.stream().map(ResolvedTemplateLayer::assignmentId).toList())
                .containsExactlyElementsOf(first.stream().map(ResolvedTemplateLayer::assignmentId).toList());
    }

    @Test
    void resolveFailsWhenTemplateHasNoVersions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = createTemplate("No Versions Template", now);
        Instance instance = createInstance("instance-no-versions", now);

        instanceTemplateAssignmentRepository.save(new InstanceTemplateAssignment(
                instance,
                template,
                null,
                0
        ));

        assertThatThrownBy(() -> resolver.resolveForInstance(instance.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Template has no versions");
    }

    private Template createTemplate(String name, OffsetDateTime now) {
        return templateRepository.save(new Template(name, "desc", TemplateType.CUSTOM, now, CREATOR_USERNAME));
    }

    private TemplateVersion createTemplateVersion(Template template, String version, OffsetDateTime createdAt) {
        TemplateVersion templateVersion = new TemplateVersion(
                template,
                version,
                "checksum",
                "s3/key",
                null,
                createdAt
        );
        return templateVersionRepository.save(templateVersion);
    }

    private Instance createInstance(String name, OffsetDateTime now) {
        Instance instance = new Instance(
                name,
                name,
                InstanceState.REQUESTED,
                UUID.randomUUID(),
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
}
