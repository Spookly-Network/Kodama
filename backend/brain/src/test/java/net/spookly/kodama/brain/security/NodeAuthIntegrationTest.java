package net.spookly.kodama.brain.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.NodeCallbackController;
import net.spookly.kodama.brain.controller.NodeController;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeRegistrationResponse;
import net.spookly.kodama.brain.service.InstanceService;
import net.spookly.kodama.brain.service.NodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {NodeController.class, NodeCallbackController.class})
@EnableConfigurationProperties(BrainSecurityProperties.class)
@Import({
        SecurityConfig.class,
        MethodSecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class,
        NodeAuthFilter.class,
        TestSecurityBootstrapConfig.class
})
@TestPropertySource(properties = {
        "brain.security.enabled=true",
        "brain.security.jwt.issuer=kodama-test",
        "brain.security.jwt.secret=01234567890123456789012345678901",
        "brain.security.jwt.token-ttl-seconds=3600",
        "brain.security.node.token=test-node-token",
        "brain.security.users[0].username=admin",
        "brain.security.users[0].display-name=Admin",
        "brain.security.users[0].email=admin@example.com",
        "brain.security.users[0].password={noop}test-pass",
        "brain.security.users[0].roles=ADMIN"
})
class NodeAuthIntegrationTest {

    private static final String NODE_TOKEN_HEADER = "X-Node-Token";
    private static final String NODE_TOKEN_VALUE = "test-node-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NodeService nodeService;

    @MockitoBean
    private InstanceService instanceService;

    @Test
    void registerNodeWithoutTokenIsUnauthorized() throws Exception {
        String body = """
                {
                  "name": "node-1",
                  "region": "eu-west",
                  "capacitySlots": 10,
                  "nodeVersion": "1.0.0",
                  "devMode": false
                }
                """;

        mockMvc.perform(post("/api/nodes/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerNodeWithTokenIsOk() throws Exception {
        given(nodeService.registerNode(org.mockito.ArgumentMatchers.any()))
                .willReturn(new NodeRegistrationResponse(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        30
                ));

        String body = """
                {
                  "name": "node-1",
                  "region": "eu-west",
                  "capacitySlots": 10,
                  "nodeVersion": "1.0.0",
                  "devMode": false
                }
                """;

        mockMvc.perform(post("/api/nodes/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(NODE_TOKEN_HEADER, NODE_TOKEN_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void registerNodeWithUserTokenIsUnauthorized() throws Exception {
        UserPrincipal principal = new UserPrincipal("admin", "Admin", "admin@example.com", Set.of(Role.ADMIN));
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        String body = """
                {
                  "name": "node-1",
                  "region": "eu-west",
                  "capacitySlots": 10,
                  "nodeVersion": "1.0.0",
                  "devMode": false
                }
                """;

        mockMvc.perform(post("/api/nodes/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(authenticationToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void heartbeatWithTokenIsOk() throws Exception {
        given(nodeService.heartbeat(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(new NodeDto(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "node-1",
                "eu-west",
                NodeStatus.ONLINE,
                false,
                10,
                2,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                "1.0.0",
                null,
                null
        ));

        String body = """
                {
                  "status": "ONLINE",
                  "usedSlots": 2
                }
                """;

        mockMvc.perform(post("/api/nodes/00000000-0000-0000-0000-000000000001/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(NODE_TOKEN_HEADER, NODE_TOKEN_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void preparedCallbackWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/nodes/00000000-0000-0000-0000-000000000001/instances"
                        + "/00000000-0000-0000-0000-000000000002/prepared"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preparedCallbackWithTokenIsOk() throws Exception {
        mockMvc.perform(post("/api/nodes/00000000-0000-0000-0000-000000000001/instances"
                        + "/00000000-0000-0000-0000-000000000002/prepared")
                        .header(NODE_TOKEN_HEADER, NODE_TOKEN_VALUE))
                .andExpect(status().isOk());
    }
}
