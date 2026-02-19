package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InstanceServiceValidationTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;

    @Mock
    private InstanceEventRepository instanceEventRepository;

    @Mock
    private InstanceStateMachine instanceStateMachine;

    @Mock
    private CommandDispatcherService commandDispatcherService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateVersionRepository templateVersionRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private TemplateAssignmentResolver templateAssignmentResolver;

    @Mock
    private SchedulingService schedulingService;

    private InstanceService instanceService;

    @BeforeEach
    void setUp() {
        instanceService = new InstanceService(
                instanceRepository,
                instanceTemplateAssignmentRepository,
                instanceEventRepository,
                instanceStateMachine,
                commandDispatcherService,
                entityManager,
                new ObjectMapper(),
                templateRepository,
                templateVersionRepository,
                nodeRepository,
                templateAssignmentResolver,
                schedulingService
        );
    }

    @Test
    void createInstanceRejectsWhenResolvedTemplateLayersAreEmpty() {
        UUID templateId = UUID.fromString("00000000-0000-0000-0000-000000002001");
        UUID versionId = UUID.fromString("00000000-0000-0000-0000-000000002002");
        UUID nodeId = UUID.fromString("00000000-0000-0000-0000-000000002003");
        UUID instanceId = UUID.fromString("00000000-0000-0000-0000-000000002004");

        Template template = new Template(
                "Base Template",
                "template description",
                TemplateType.CUSTOM,
                OffsetDateTime.now(ZoneOffset.UTC),
                "tester"
        );
        ReflectionTestUtils.setField(template, "id", templateId);

        TemplateVersion version = new TemplateVersion(
                template,
                "1.0.0",
                "checksum",
                "s3/base-template-1.0.0.tar.gz",
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(version, "id", versionId);

        Node node = new Node(
                "node-test",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                5,
                0,
                OffsetDateTime.now(ZoneOffset.UTC),
                "1.0.0",
                null,
                "http://node.test"
        );
        ReflectionTestUtils.setField(node, "id", nodeId);

        CreateInstanceRequest request = new CreateInstanceRequest(
                "instance-empty-resolved-layers",
                List.of(new TemplateAssignmentRequest(templateId, versionId, 0))
        );
        request.setNodeId(nodeId);

        when(instanceRepository.findByName(request.getName())).thenReturn(Optional.empty());
        when(templateRepository.findAllById(any())).thenReturn(List.of(template));
        when(templateVersionRepository.findAllById(any())).thenReturn(List.of(version));
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(invocation -> {
            Instance saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", instanceId);
            return saved;
        });
        when(instanceTemplateAssignmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceEventRepository.save(any(InstanceEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(templateAssignmentResolver.resolveForInstance(instanceId)).thenReturn(List.of());

        assertThatThrownBy(() -> instanceService.createInstance(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("template layers are required");
                });
    }
}
