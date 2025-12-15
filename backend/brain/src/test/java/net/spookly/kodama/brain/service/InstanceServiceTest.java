package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
@Import(InstanceService.class)
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

    @Test
    void createInstancePersistsLayersAndRequestedEvent() {
        TemplateVersion version = createTemplateVersion("Base Template", "1.0.0");

        CreateInstanceRequest request = new CreateInstanceRequest(
                "instance-one",
                "Instance One",
                REQUESTER_ID,
                null,
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0)),
                "{\"SERVER\":\"alpha\"}",
                "{\"PORT\":25565}"
        );

        InstanceDto created = instanceService.createInstance(request);

        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getState()).isEqualTo(InstanceState.REQUESTED);
        assertThat(persisted.getVariablesJson()).contains("SERVER");

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
    void createInstanceRejectsDuplicateNames() {
        TemplateVersion version = createTemplateVersion("Dupe Template", "1.0.0");
        CreateInstanceRequest request = new CreateInstanceRequest(
                "duplicate-instance",
                null,
                REQUESTER_ID,
                null,
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0)),
                null,
                null
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
                null,
                REQUESTER_ID,
                null,
                List.of(new InstanceTemplateLayerRequest(UUID.randomUUID(), 0)),
                null,
                null
        );

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
                OffsetDateTime.now(ZoneOffset.UTC),
                10
        ));
        TemplateVersion version = createTemplateVersion("Node Template", "1.0.0");

        CreateInstanceRequest request = new CreateInstanceRequest(
                "with-node",
                null,
                REQUESTER_ID,
                node.getId(),
                List.of(new InstanceTemplateLayerRequest(version.getId(), 0)),
                null,
                null
        );

        InstanceDto created = instanceService.createInstance(request);
        Instance persisted = instanceRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getNode()).isNotNull();
        assertThat(persisted.getNode().getId()).isEqualTo(node.getId());
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
