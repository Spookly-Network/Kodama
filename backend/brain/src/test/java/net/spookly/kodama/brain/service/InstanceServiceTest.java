package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.dto.InstanceTemplateLayerRequest;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateLayerRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstanceService.class, InstanceStateMachine.class, InstanceServiceTest.ObjectMapperTestConfig.class})
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

    private static final UUID REQUESTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private InstanceTemplateLayerRepository instanceTemplateLayerRepository;

    @Autowired
    private InstanceEventRepository instanceEventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @TestConfiguration
    static class ObjectMapperTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void createInstancePersistsLayersAndRequestedEvent() {
        TemplateVersion version = createTemplateVersion("Base Template", "1.0.0");

        CreateInstanceRequest request = new CreateInstanceRequest(
                "instance-one",
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0))
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

        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceId(created.getId());
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0).getTemplateVersion().getId()).isEqualTo(version.getId());
        assertThat(layers.get(0).getOrderIndex()).isZero();

        List<InstanceEvent> events =
                instanceEventRepository.findAllByInstanceIdOrderByTimestampAsc(created.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getType()).isEqualTo(InstanceEventType.REQUEST_RECEIVED);
    }

    @Test
    void createInstanceAppliesListOrderWhenOrderIndexMissing() {
        TemplateVersion base = createTemplateVersion("Base Template", "1.0.0");
        TemplateVersion overlay = createTemplateVersion("Overlay Template", "1.0.0");

        InstanceTemplateLayerRequest baseLayer = new InstanceTemplateLayerRequest();
        baseLayer.setTemplateVersionId(base.getId());
        InstanceTemplateLayerRequest overlayLayer = new InstanceTemplateLayerRequest();
        overlayLayer.setTemplateVersionId(overlay.getId());

        CreateInstanceRequest request = new CreateInstanceRequest(
                "ordered-instance",
                List.of(baseLayer, overlayLayer)
        );
        request.setDisplayName("Ordered");
        request.setRequestedBy(REQUESTER_ID);

        InstanceDto created = instanceService.createInstance(request);
        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceId(created.getId());

        assertThat(layers).hasSize(2);
        assertThat(layers.get(0).getTemplateVersion().getId()).isEqualTo(base.getId());
        assertThat(layers.get(0).getOrderIndex()).isZero();
        assertThat(layers.get(1).getTemplateVersion().getId()).isEqualTo(overlay.getId());
        assertThat(layers.get(1).getOrderIndex()).isEqualTo(1);
    }

    @Test
    void createInstanceUsesLatestTemplateVersionWhenOnlyTemplateIdProvided() {
        Template template = createTemplate("Template With Versions");
        OffsetDateTime earlier = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        TemplateVersion first = templateVersionRepository.save(new TemplateVersion(
                template, "1.0.0", "checksum-1", "s3/key/1", null, earlier));
        TemplateVersion second = templateVersionRepository.save(new TemplateVersion(
                template, "1.1.0", "checksum-2", "s3/key/2", null, OffsetDateTime.now(ZoneOffset.UTC)));

        InstanceTemplateLayerRequest layer = new InstanceTemplateLayerRequest();
        layer.setTemplateId(template.getId());

        CreateInstanceRequest request = new CreateInstanceRequest(
                "latest-version-instance",
                List.of(layer)
        );
        request.setRequestedBy(REQUESTER_ID);

        InstanceDto created = instanceService.createInstance(request);
        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceId(created.getId());

        assertThat(layers).hasSize(1);
        assertThat(layers.getFirst().getTemplateVersion().getId()).isEqualTo(second.getId());
        assertThat(layers.getFirst().getOrderIndex()).isZero();
        assertThat(second.getCreatedAt()).isAfter(first.getCreatedAt());
    }

    @Test
    void createInstanceRejectsDuplicateNames() {
        TemplateVersion version = createTemplateVersion("Dupe Template", "1.0.0");
        CreateInstanceRequest request = new CreateInstanceRequest(
                "duplicate-instance",
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0))
        );


        instanceService.createInstance(request);
        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createInstanceFailsWhenTemplateVersionMissing() {
        CreateInstanceRequest request = new CreateInstanceRequest(
                "missing-template-version",
                List.of(new InstanceTemplateLayerRequest(UUID.randomUUID(), 0))
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

        CreateInstanceRequest request = new CreateInstanceRequest(
                "with-node",
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0))
        );
        request.setRequestedBy(REQUESTER_ID);
        request.setNodeId(node.getId());

        InstanceDto created = instanceService.createInstance(request);
        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getNode()).isNotNull();
        assertThat(persisted.getNode().getId()).isEqualTo(node.getId());
    }

    @Test
    void createInstanceRejectsVariablesAndVariablesJsonTogether() {
        TemplateVersion version = createTemplateVersion("Mixed Variables", "1.0.0");
        CreateInstanceRequest request = new CreateInstanceRequest(
                "invalid-variables",
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0))
        );
        request.setVariables(Map.of("ENV", "prod"));
        request.setVariablesJson("{\"ENV\":\"prod\"}");

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

    private Template createTemplate(String name) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return templateRepository.save(new Template(name, "desc", TemplateType.CUSTOM, now, REQUESTER_ID));
    }

    private TemplateVersion createTemplateVersion(String templateName, String version) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = new Template(templateName, "desc", TemplateType.CUSTOM, now, REQUESTER_ID);
        Template savedTemplate = templateRepository.save(template);

        TemplateVersion templateVersion = new TemplateVersion(
                savedTemplate, version, "checksum", "s3/key", null, now);
        return templateVersionRepository.save(templateVersion);
    }
}
