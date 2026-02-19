package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintGroupLink;
import net.spookly.kodama.brain.domain.blueprint.BlueprintPortDefinition;
import net.spookly.kodama.brain.domain.blueprint.BlueprintTemplateAssignment;
import net.spookly.kodama.brain.domain.blueprint.PortProtocol;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.config.PluginsProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.dto.PortDefinitionRequest;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.BlueprintGroupLinkRepository;
import net.spookly.kodama.brain.repository.BlueprintPortDefinitionRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.BlueprintTemplateAssignmentRepository;
import net.spookly.kodama.brain.plugin.BrainPluginRegistry;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        InstanceService.class,
        InstanceCreationPreparationService.class,
        BlueprintService.class,
        TemplateAssignmentService.class,
        BlueprintPortDefinitionService.class,
        BlueprintGroupLinkService.class,
        TemplateService.class,
        InstanceGroupService.class,
        InstanceStateMachine.class,
        SchedulingService.class,
        TemplateAssignmentResolver.class,
        InstanceServiceTest.ObjectMapperTestConfig.class
})
class InstanceServiceTest {

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
    private InstanceService instanceService;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;

    @Autowired
    private InstanceEventRepository instanceEventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private BlueprintTemplateAssignmentRepository blueprintTemplateAssignmentRepository;

    @Autowired
    private BlueprintPortDefinitionRepository blueprintPortDefinitionRepository;

    @Autowired
    private BlueprintGroupLinkRepository blueprintGroupLinkRepository;

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private InstanceGroupMembershipRepository instanceGroupMembershipRepository;

    @TestConfiguration
    static class ObjectMapperTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CommandDispatcherService commandDispatcherService(ObjectMapper objectMapper) {
            return new CommandDispatcherService(
                    new RestTemplate(),
                    new NodeProperties(),
                    new BrainPluginRegistry(new PluginsProperties(), objectMapper),
                    new BrainSecurityProperties(),
                    objectMapper
            );
        }
    }

    @Test
    void createInstancePersistsLayersAndRequestedEvent() {
        createOnlineNode(
                "node-primary",
                "eu-west-1",
                true,
                "primary,ssd",
                10,
                0
        );
        TemplateVersion version = createTemplateVersion("Base Template", "1.0.0");

        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "instance-one",
                List.of(assignment)
        );
        request.setDisplayName("Instance One");
        request.setRequestedBy(REQUESTER_ID);
        request.setRegion("eu-west-1");
        request.setTags("primary,ssd");
        request.setDevModeAllowed(Boolean.TRUE);
        request.setVariables(Map.of("SERVER", "alpha"));
        request.setPortsJson("{\"PORT\":25565}");

        InstanceDto created = instanceService.createInstance(request);

        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.REQUESTED);
        assertThat(persisted.getVariablesJson()).contains("SERVER");
        assertThat(persisted.getRegion()).isEqualTo("eu-west-1");
        assertThat(persisted.getTags()).isEqualTo("primary,ssd");
        assertThat(persisted.getDevModeAllowed()).isTrue();

        List<InstanceTemplateAssignment> assignments =
                instanceTemplateAssignmentRepository.findAllByInstanceId(created.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getTemplate().getId()).isEqualTo(version.getTemplate().getId());
        assertThat(assignments.getFirst().getTemplateVersion().getId()).isEqualTo(version.getId());
        assertThat(assignments.getFirst().getPriority()).isZero();

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(created.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getType()).isEqualTo(InstanceEventType.REQUEST_RECEIVED);
    }

    @Test
    void createInstanceAppliesListOrderWhenOrderIndexMissing() {
        createOnlineNode("node-ordering");
        TemplateVersion base = createTemplateVersion("Base Template", "1.0.0");
        TemplateVersion overlay = createTemplateVersion("Overlay Template", "1.0.0");

        TemplateAssignmentRequest baseLayer = new TemplateAssignmentRequest();
        baseLayer.setTemplateId(base.getTemplate().getId());
        baseLayer.setTemplateVersionId(base.getId());
        TemplateAssignmentRequest overlayLayer = new TemplateAssignmentRequest();
        overlayLayer.setTemplateId(overlay.getTemplate().getId());
        overlayLayer.setTemplateVersionId(overlay.getId());

        CreateInstanceRequest request = new CreateInstanceRequest(
                "ordered-instance",
                List.of(baseLayer, overlayLayer)
        );
        request.setDisplayName("Ordered");
        request.setRequestedBy(REQUESTER_ID);

        InstanceDto created = instanceService.createInstance(request);
        assertThat(created.getTemplateLayers()).hasSize(2);
        assertThat(created.getTemplateLayers().get(0).getTemplateVersionId()).isEqualTo(base.getId());
        assertThat(created.getTemplateLayers().get(0).getPriority()).isZero();
        assertThat(created.getTemplateLayers().get(0).getOrderIndex()).isZero();
        assertThat(created.getTemplateLayers().get(1).getTemplateVersionId()).isEqualTo(overlay.getId());
        assertThat(created.getTemplateLayers().get(1).getPriority()).isEqualTo(1);
        assertThat(created.getTemplateLayers().get(1).getOrderIndex()).isEqualTo(1);
    }

    @Test
    void createInstanceUsesLatestTemplateVersionWhenOnlyTemplateIdProvided() {
        createOnlineNode("node-latest");
        Template template = createTemplate("Template With Versions");
        OffsetDateTime earlier = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        TemplateVersion first = templateVersionRepository.save(new TemplateVersion(
                template, "1.0.0", "checksum-1", "s3/key/1", null, earlier));
        TemplateVersion second = templateVersionRepository.save(new TemplateVersion(
                template, "1.1.0", "checksum-2", "s3/key/2", null, OffsetDateTime.now(ZoneOffset.UTC)));

        TemplateAssignmentRequest layer = new TemplateAssignmentRequest();
        layer.setTemplateId(template.getId());

        CreateInstanceRequest request = new CreateInstanceRequest(
                "latest-version-instance",
                List.of(layer)
        );
        request.setRequestedBy(REQUESTER_ID);

        InstanceDto created = instanceService.createInstance(request);
        assertThat(created.getTemplateLayers()).hasSize(1);
        assertThat(created.getTemplateLayers().getFirst().getTemplateVersionId()).isEqualTo(second.getId());
        assertThat(created.getTemplateLayers().getFirst().getPriority()).isZero();
        assertThat(created.getTemplateLayers().getFirst().getOrderIndex()).isZero();
        assertThat(second.getCreatedAt()).isAfter(first.getCreatedAt());
    }

    @Test
    void createInstanceRejectsTemplateWithoutVersionsWhenVersionOmitted() {
        Template template = createTemplate("Template Without Versions");
        TemplateAssignmentRequest layer = new TemplateAssignmentRequest();
        layer.setTemplateId(template.getId());

        CreateInstanceRequest request = new CreateInstanceRequest(
                "no-version-instance",
                List.of(layer)
        );
        request.setRequestedBy(REQUESTER_ID);

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createInstanceAllowsDuplicatePriorities() {
        createOnlineNode("node-duplicate-priority");
        TemplateVersion base = createTemplateVersion("Priority Base", "1.0.0");
        TemplateVersion overlay = createTemplateVersion("Priority Overlay", "1.0.0");

        TemplateAssignmentRequest baseLayer = new TemplateAssignmentRequest(
                base.getTemplate().getId(),
                base.getId(),
                0
        );
        TemplateAssignmentRequest overlayLayer = new TemplateAssignmentRequest(
                overlay.getTemplate().getId(),
                overlay.getId(),
                0
        );

        CreateInstanceRequest request = new CreateInstanceRequest(
                "duplicate-priority-instance",
                List.of(baseLayer, overlayLayer)
        );
        request.setRequestedBy(REQUESTER_ID);

        InstanceDto created = instanceService.createInstance(request);

        List<InstanceTemplateAssignment> assignments =
                instanceTemplateAssignmentRepository.findAllByInstanceId(created.getId());
        assertThat(assignments).hasSize(2);
        assertThat(assignments).allMatch(assignment -> assignment.getPriority() == 0);
    }

    @Test
    void createInstanceRejectsDuplicateNames() {
        createOnlineNode("node-duplicate-names");
        TemplateVersion version = createTemplateVersion("Dupe Template", "1.0.0");
        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "duplicate-instance",
                List.of(assignment)
        );


        instanceService.createInstance(request);
        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createInstanceRejectsEmptyTemplateLayers() {
        CreateInstanceRequest request = new CreateInstanceRequest(
                "empty-template-layers",
                List.of()
        );

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("template layers are required");
                });
    }

    @Test
    void createInstanceFailsWhenTemplateVersionMissing() {
        Template template = createTemplate("Missing Version Template");
        CreateInstanceRequest request = new CreateInstanceRequest(
                "missing-template-version",
                List.of(new TemplateAssignmentRequest(template.getId(), UUID.randomUUID(), 0))
        );
        request.setRequestedBy(REQUESTER_ID);

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createInstanceAssignsNodeWhenProvided() {
        Node node = nodeRepository.save(new Node(
                "node-1",
                "eu-west",
                NodeStatus.ONLINE,
                false,
                10,
                0,
                OffsetDateTime.now(ZoneOffset.UTC),
                "1.0.0",
                "primary,ssd",
                "http://node-1.internal"
        ));
        TemplateVersion version = createTemplateVersion("Node Template", "1.0.0");

        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "with-node",
                List.of(assignment)
        );
        request.setRequestedBy(REQUESTER_ID);
        request.setNodeId(node.getId());

        InstanceDto created = instanceService.createInstance(request);
        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getNode()).isNotNull();
        assertThat(persisted.getNode().getId()).isEqualTo(node.getId());
    }

    @Test
    void createInstanceSelectsNodeWhenNodeIdMissing() {
        Node selectedNode = createOnlineNode(
                "node-low",
                "eu-west-1",
                false,
                "primary,ssd",
                10,
                1
        );
        createOnlineNode(
                "node-high",
                "eu-west-1",
                false,
                "primary,ssd",
                10,
                5
        );
        TemplateVersion version = createTemplateVersion("Schedule Template", "1.0.0");

        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "scheduled-instance",
                List.of(assignment)
        );
        request.setRegion("eu-west-1");
        request.setTags("primary,ssd");
        request.setDevModeAllowed(Boolean.FALSE);

        InstanceDto created = instanceService.createInstance(request);
        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getNode()).isNotNull();
        assertThat(persisted.getNode().getId()).isEqualTo(selectedNode.getId());
    }

    @Test
    void createInstanceRejectsWhenNoEligibleNodesAvailable() {
        TemplateVersion version = createTemplateVersion("No Node Template", "1.0.0");

        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "no-node",
                List.of(assignment)
        );

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createInstanceRejectsVariablesAndVariablesJsonTogether() {
        createOnlineNode("node-variables");
        TemplateVersion version = createTemplateVersion("Mixed Variables", "1.0.0");
        TemplateAssignmentRequest assignment = new TemplateAssignmentRequest(
                version.getTemplate().getId(),
                version.getId(),
                0
        );
        CreateInstanceRequest request = new CreateInstanceRequest(
                "invalid-variables",
                List.of(assignment)
        );
        request.setVariables(Map.of("ENV", "prod"));
        request.setVariablesJson("{\"ENV\":\"prod\"}");

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createInstanceWithBlueprintUsesBlueprintDefaults() throws Exception {
        createOnlineNode("node-blueprint-defaults");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TemplateVersion version = createTemplateVersion("Blueprint Base Layer", "1.0.0");
        InstanceGroup blueprintGroup = instanceGroupRepository.save(new InstanceGroup(
                "bp-default-group",
                "group from blueprint",
                now,
                now
        ));
        Blueprint blueprint = createBlueprint(
                "bp-defaults",
                false,
                2,
                "ghcr.io/spookly/hytale:default",
                "echo install defaults",
                List.of("./start-default.sh"),
                "{\"MODE\":\"default\"}",
                now
        );
        blueprintTemplateAssignmentRepository.save(new BlueprintTemplateAssignment(
                blueprint,
                version.getTemplate(),
                version,
                3
        ));
        blueprintPortDefinitionRepository.save(new BlueprintPortDefinition(
                blueprint,
                "game",
                PortProtocol.TCP,
                25565,
                30000,
                30100,
                1
        ));
        blueprintGroupLinkRepository.save(new BlueprintGroupLink(blueprint, blueprintGroup));

        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setName("instance-blueprint-defaults");
        request.setBlueprintId(blueprint.getId());

        InstanceDto created = instanceService.createInstance(request);

        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getBlueprint()).isNotNull();
        assertThat(persisted.getBlueprint().getId()).isEqualTo(blueprint.getId());
        assertThat(persisted.getPermanent()).isFalse();
        assertThat(persisted.getSlotsRequired()).isEqualTo(2);
        assertThat(persisted.getContainerImage()).isEqualTo("ghcr.io/spookly/hytale:default");
        assertThat(persisted.getInstallScript()).isEqualTo("echo install defaults");
        assertThat(persisted.getVariablesJson()).isEqualTo("{\"MODE\":\"default\"}");
        assertThat(objectMapper.readValue(persisted.getStartCommandJson(), new TypeReference<List<String>>() {
        })).containsExactly("./start-default.sh");

        List<Map<String, Object>> portDefinitions = objectMapper.readValue(
                persisted.getPortDefinitionsJson(),
                new TypeReference<>() {
                }
        );
        assertThat(portDefinitions).hasSize(1);
        assertThat(portDefinitions.getFirst()).containsEntry("name", "game");

        List<InstanceTemplateAssignment> assignments =
                instanceTemplateAssignmentRepository.findAllByInstanceId(created.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getTemplate().getId()).isEqualTo(version.getTemplate().getId());
        assertThat(assignments.getFirst().getPriority()).isEqualTo(3);

        List<InstanceGroupMembership> memberships = instanceGroupMembershipRepository.findAllByInstanceId(created.getId());
        assertThat(memberships).hasSize(1);
        assertThat(memberships.getFirst().getGroup().getId()).isEqualTo(blueprintGroup.getId());
    }

    @Test
    void createInstanceWithBlueprintOverridesReplaceDefaults() throws Exception {
        createOnlineNode("node-blueprint-overrides");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TemplateVersion blueprintVersion = createTemplateVersion("Blueprint Layer", "1.0.0");
        TemplateVersion overrideVersion = createTemplateVersion("Override Layer", "2.0.0");

        InstanceGroup blueprintGroup = instanceGroupRepository.save(new InstanceGroup(
                "bp-override-group",
                "default group",
                now,
                now
        ));
        InstanceGroup overrideGroup = instanceGroupRepository.save(new InstanceGroup(
                "bp-override-group-new",
                "override group",
                now,
                now
        ));

        Blueprint blueprint = createBlueprint(
                "bp-overrides",
                false,
                1,
                "ghcr.io/spookly/hytale:blueprint",
                "echo install blueprint",
                List.of("./start-blueprint.sh"),
                "{\"MODE\":\"blueprint\"}",
                now
        );
        blueprintTemplateAssignmentRepository.save(new BlueprintTemplateAssignment(
                blueprint,
                blueprintVersion.getTemplate(),
                blueprintVersion,
                0
        ));
        blueprintPortDefinitionRepository.save(new BlueprintPortDefinition(
                blueprint,
                "query",
                PortProtocol.UDP,
                25566,
                31000,
                31100,
                1
        ));
        blueprintGroupLinkRepository.save(new BlueprintGroupLink(blueprint, blueprintGroup));

        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setName("instance-blueprint-overrides");
        request.setBlueprintId(blueprint.getId());
        request.setPermanent(Boolean.TRUE);
        request.setSlotsRequired(6);
        request.setContainerImage("ghcr.io/spookly/hytale:override");
        request.setInstallScript("echo install override");
        request.setStartCommand(List.of("./start-override.sh", "--safe-mode"));
        request.setVariables(Map.of("MODE", "override"));
        request.setTemplateLayers(List.of(new TemplateAssignmentRequest(
                overrideVersion.getTemplate().getId(),
                overrideVersion.getId(),
                4
        )));
        request.setPortDefinitions(List.of(new PortDefinitionRequest(
                "game",
                "tcp",
                25565,
                new PortDefinitionRequest.HostRangeRequest(32000, 32100, 1)
        )));
        request.setGroupIds(List.of(overrideGroup.getId()));

        InstanceDto created = instanceService.createInstance(request);

        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getBlueprint()).isNotNull();
        assertThat(persisted.getBlueprint().getId()).isEqualTo(blueprint.getId());
        assertThat(persisted.getPermanent()).isTrue();
        assertThat(persisted.getSlotsRequired()).isEqualTo(6);
        assertThat(persisted.getContainerImage()).isEqualTo("ghcr.io/spookly/hytale:override");
        assertThat(persisted.getInstallScript()).isEqualTo("echo install override");
        assertThat(objectMapper.readValue(persisted.getStartCommandJson(), new TypeReference<List<String>>() {
        })).containsExactly("./start-override.sh", "--safe-mode");
        assertThat(persisted.getVariablesJson()).contains("\"MODE\":\"override\"");

        List<Map<String, Object>> portDefinitions = objectMapper.readValue(
                persisted.getPortDefinitionsJson(),
                new TypeReference<>() {
                }
        );
        assertThat(portDefinitions).hasSize(1);
        assertThat(portDefinitions.getFirst()).containsEntry("name", "game");
        assertThat(portDefinitions.getFirst()).containsEntry("protocol", "tcp");

        List<InstanceTemplateAssignment> assignments =
                instanceTemplateAssignmentRepository.findAllByInstanceId(created.getId());
        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getTemplate().getId()).isEqualTo(overrideVersion.getTemplate().getId());
        assertThat(assignments.getFirst().getPriority()).isEqualTo(4);

        List<InstanceGroupMembership> memberships = instanceGroupMembershipRepository.findAllByInstanceId(created.getId());
        assertThat(memberships).hasSize(1);
        assertThat(memberships.getFirst().getGroup().getId()).isEqualTo(overrideGroup.getId());
    }

    @Test
    void createInstanceRejectsBlueprintWithoutTemplateAssignments() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint(
                "bp-empty-assignments",
                false,
                1,
                "ghcr.io/spookly/hytale:empty-assignments",
                null,
                List.of("./start.sh"),
                null,
                now
        );

        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setName("instance-blueprint-empty-assignments");
        request.setBlueprintId(blueprint.getId());

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusException = (ResponseStatusException) ex;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusException.getReason()).isEqualTo("template layers are required");
                });
    }

    @Test
    void createInstanceRejectsDeletedBlueprint() throws Exception {
        createOnlineNode("node-blueprint-deleted");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint(
                "bp-deleted",
                false,
                1,
                "ghcr.io/spookly/hytale:deleted",
                null,
                List.of("./start.sh"),
                null,
                now
        );
        blueprint.softDelete(now.plusMinutes(1));
        blueprintRepository.flush();

        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setName("instance-blueprint-deleted");
        request.setBlueprintId(blueprint.getId());

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusException = (ResponseStatusException) ex;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusException.getReason()).contains("deleted");
                });
    }

    @Test
    void createInstanceWithoutBlueprintStillRequiresTemplateLayers() {
        createOnlineNode("node-legacy-validation");
        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setName("legacy-requires-template-layers");

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reportPreparedUpdatesStateAndLogsEvent() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-callback",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node.local"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-callback",
                "Callback Instance",
                InstanceState.PREPARING,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        ));

        instanceService.reportInstancePrepared(node.getId(), instance.getId());

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.STARTING);

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(instance.getId());
        assertThat(events).isNotEmpty();
        assertThat(events.getLast().getType()).isEqualTo(InstanceEventType.PREPARE_COMPLETED);
    }

    @Test
    void reportPreparedPersistsPortsJsonWhenProvided() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-callback-ports",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node.local"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-callback-ports",
                "Callback Ports Instance",
                InstanceState.PREPARING,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                "{\"old\":25565}",
                null,
                now,
                now
        ));

        instanceService.reportInstancePrepared(node.getId(), instance.getId(), "{\"game\":30000}");

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.STARTING);
        assertThat(persisted.getPortsJson()).isEqualTo("{\"game\":30000}");
    }

    @Test
    void reportPreparedFromRequestedTransitionsToStarting() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-fast-callback",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node.fast"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-fast-callback",
                "Fast Callback Instance",
                InstanceState.REQUESTED,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        ));

        instanceService.reportInstancePrepared(node.getId(), instance.getId());

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.STARTING);

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(instance.getId());
        assertThat(events).isNotEmpty();
        assertThat(events).extracting(InstanceEvent::getType)
                .contains(InstanceEventType.PREPARE_DISPATCHED, InstanceEventType.PREPARE_COMPLETED);
    }

    @Test
    void reportPreparedRejectsMismatchedNode() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-primary",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                0,
                now,
                "1.0.0",
                null,
                "http://node.primary"
        ));
        Node otherNode = nodeRepository.save(new Node(
                "node-secondary",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                0,
                now,
                "1.0.0",
                null,
                "http://node.secondary"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-wrong-node",
                "Wrong Node Instance",
                InstanceState.PREPARING,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        ));

        assertThatThrownBy(() -> instanceService.reportInstancePrepared(otherNode.getId(), instance.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reportStoppedUpdatesStateAndLogsEvent() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-stop",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node.stop"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-stop",
                "Stop Instance",
                InstanceState.STOPPING,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        ));

        instanceService.reportInstanceStopped(node.getId(), instance.getId());

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.STOPPED);

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(instance.getId());
        assertThat(events).isNotEmpty();
        assertThat(events.getLast().getType()).isEqualTo(InstanceEventType.STOP_COMPLETED);
    }

    @Test
    void reportDestroyedUpdatesStateAndLogsEvent() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.save(new Node(
                "node-destroy",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                "http://node.destroy"
        ));
        Instance instance = instanceRepository.save(new Instance(
                "instance-destroy",
                "Destroy Instance",
                InstanceState.STOPPING,
                REQUESTER_ID,
                node,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        ));

        instanceService.reportInstanceDestroyed(node.getId(), instance.getId());

        Instance persisted = instanceRepository.findById(instance.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.DESTROYED);

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(instance.getId());
        assertThat(events).isNotEmpty();
        assertThat(events.getLast().getType()).isEqualTo(InstanceEventType.DESTROY_COMPLETED);
    }

    private Blueprint createBlueprint(
            String name,
            boolean permanent,
            Integer slotsRequired,
            String containerImage,
            String installScript,
            List<String> startCommand,
            String variablesJson,
            OffsetDateTime now
    ) throws JsonProcessingException {
        return blueprintRepository.save(new Blueprint(
                name,
                permanent,
                slotsRequired,
                containerImage,
                installScript,
                objectMapper.writeValueAsString(startCommand),
                variablesJson,
                now,
                now
        ));
    }

    private Template createTemplate(String name) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return templateRepository.save(new Template(name, "desc", TemplateType.CUSTOM, now, REQUESTER_USERNAME));
    }

    private Node createOnlineNode(String name) {
        return createOnlineNode(name, "eu-west-1", false, null, 10, 0);
    }

    private Node createOnlineNode(
            String name,
            String region,
            boolean devMode,
            String tags,
            int capacitySlots,
            int usedSlots
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return nodeRepository.save(new Node(
                name,
                region,
                NodeStatus.ONLINE,
                devMode,
                capacitySlots,
                usedSlots,
                now,
                "1.0.0",
                tags,
                "http://" + name + ".local"
        ));
    }

    private TemplateVersion createTemplateVersion(String templateName, String version) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = new Template(templateName, "desc", TemplateType.CUSTOM, now, REQUESTER_USERNAME);
        Template savedTemplate = templateRepository.save(template);

        TemplateVersion templateVersion = new TemplateVersion(
                savedTemplate, version, "checksum", "s3/key", null, now);
        return templateVersionRepository.save(templateVersion);
    }
}
