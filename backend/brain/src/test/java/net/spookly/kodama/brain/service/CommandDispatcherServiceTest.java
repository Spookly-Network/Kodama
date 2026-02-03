package net.spookly.kodama.brain.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateType;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.node.NodeInstanceCommandRequest;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceLayer;
import net.spookly.kodama.brain.dto.node.NodePrepareInstanceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class CommandDispatcherServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private NodeProperties nodeProperties;
    private CommandDispatcherService dispatcher;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        nodeProperties = new NodeProperties();
        nodeProperties.setCommandRetryBackoffMillis(0);
        dispatcher = new CommandDispatcherService(restTemplate, nodeProperties);
    }

    @Test
    void sendPrepareInstanceSendsExpectedPayload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID templateVersionId = UUID.randomUUID();
        Node node = buildNode(nodeId, "http://node-1.internal");
        Instance instance = buildInstance(instanceId, node);
        InstanceTemplateLayer layer = buildLayer(instance, templateId, templateVersionId);

        Map<String, String> variables = Map.of("WORLD_NAME", "test");
        NodePrepareInstanceLayer expectedLayer = new NodePrepareInstanceLayer(
                templateVersionId,
                templateId,
                "1.0.0",
                "checksum",
                "s3://templates/template-1-1.0.0.tar.gz",
                "{\"hello\":\"world\"}",
                0
        );
        NodePrepareInstanceRequest expected = new NodePrepareInstanceRequest(
                instanceId,
                instance.getName(),
                instance.getDisplayName(),
                instance.getPortsJson(),
                variables,
                null,
                List.of(expectedLayer)
        );

        server.expect(requestTo("http://node-1.internal/api/instances/" + instanceId + "/prepare"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andRespond(withSuccess());

        dispatcher.sendPrepareInstance(node, instance, List.of(layer), variables);

        server.verify();
    }

    @Test
    void sendStartInstanceSendsExpectedPayload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Node node = buildNode(nodeId, "http://node-1.internal");
        Instance instance = buildInstance(instanceId, node);

        NodeInstanceCommandRequest expected = new NodeInstanceCommandRequest(instanceId, instance.getName());

        server.expect(requestTo("http://node-1.internal/api/instances/" + instanceId + "/start"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andRespond(withSuccess());

        dispatcher.sendStartInstance(node, instance);

        server.verify();
    }

    @Test
    void sendStopInstanceSendsExpectedPayload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Node node = buildNode(nodeId, "http://node-1.internal");
        Instance instance = buildInstance(instanceId, node);

        NodeInstanceCommandRequest expected = new NodeInstanceCommandRequest(instanceId, instance.getName());

        server.expect(requestTo("http://node-1.internal/api/instances/" + instanceId + "/stop"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andRespond(withSuccess());

        dispatcher.sendStopInstance(node, instance);

        server.verify();
    }

    @Test
    void sendDestroyInstanceSendsExpectedPayload() throws Exception {
        UUID nodeId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Node node = buildNode(nodeId, "http://node-1.internal");
        Instance instance = buildInstance(instanceId, node);

        NodeInstanceCommandRequest expected = new NodeInstanceCommandRequest(instanceId, instance.getName());

        server.expect(requestTo("http://node-1.internal/api/instances/" + instanceId + "/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andRespond(withSuccess());

        dispatcher.sendDestroyInstance(node, instance);

        server.verify();
    }

    @Test
    void retriesOnServerError() {
        UUID nodeId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Node node = buildNode(nodeId, "http://node-1.internal");
        Instance instance = buildInstance(instanceId, node);

        AtomicInteger attempts = new AtomicInteger();
        server.expect(ExpectedCount.times(2), requestTo("http://node-1.internal/api/instances/" + instanceId + "/start"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    if (attempts.getAndIncrement() == 0) {
                        return withStatus(HttpStatus.SERVICE_UNAVAILABLE).createResponse(request);
                    }
                    return withSuccess().createResponse(request);
                });

        dispatcher.sendStartInstance(node, instance);

        server.verify();
    }

    private Node buildNode(UUID nodeId, String baseUrl) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = new Node(
                "node-1",
                "eu-west-1",
                NodeStatus.ONLINE,
                false,
                4,
                1,
                now,
                "1.0.0",
                null,
                baseUrl
        );
        ReflectionTestUtils.setField(node, "id", nodeId);
        return node;
    }

    private Instance buildInstance(UUID instanceId, Node node) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = new Instance(
                "instance-1",
                "Instance 1",
                InstanceState.REQUESTED,
                UUID.randomUUID(),
                node,
                node.getRegion(),
                null,
                false,
                null,
                null,
                now,
                now
        );
        ReflectionTestUtils.setField(instance, "id", instanceId);
        return instance;
    }

    private InstanceTemplateLayer buildLayer(Instance instance, UUID templateId, UUID templateVersionId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Template template = new Template(
                "template-1",
                "Template 1",
                TemplateType.CUSTOM,
                now,
                UUID.randomUUID()
        );
        ReflectionTestUtils.setField(template, "id", templateId);
        TemplateVersion version = new TemplateVersion(
                template,
                "1.0.0",
                "checksum",
                "s3://templates/template-1-1.0.0.tar.gz",
                "{\"hello\":\"world\"}",
                now
        );
        ReflectionTestUtils.setField(version, "id", templateVersionId);
        return new InstanceTemplateLayer(instance, version, 0);
    }
}
