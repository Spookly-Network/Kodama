package net.spookly.kodama.brain.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.AuthController;
import net.spookly.kodama.brain.controller.InstanceController;
import net.spookly.kodama.brain.controller.TemplateController;
import net.spookly.kodama.brain.service.AuthService;
import net.spookly.kodama.brain.service.InstanceService;
import net.spookly.kodama.brain.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, InstanceController.class, TemplateController.class})
@EnableConfigurationProperties(BrainSecurityProperties.class)
@Import({
        SecurityConfig.class,
        MethodSecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class,
        ConfiguredUserStore.class,
        AuthService.class
})
@TestPropertySource(properties = {
        "brain.security.enabled=true",
        "brain.security.jwt.issuer=kodama-test",
        "brain.security.jwt.secret=01234567890123456789012345678901",
        "brain.security.jwt.token-ttl-seconds=3600",
        "brain.security.users[0].username=admin",
        "brain.security.users[0].display-name=Admin",
        "brain.security.users[0].email=admin@example.com",
        "brain.security.users[0].password={noop}test-pass",
        "brain.security.users[0].roles=ADMIN",
        "brain.security.users[1].username=viewer",
        "brain.security.users[1].display-name=Viewer",
        "brain.security.users[1].email=viewer@example.com",
        "brain.security.users[1].password={noop}view-pass",
        "brain.security.users[1].roles=VIEWER"
})
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private ConfiguredUserStore userStore;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        given(instanceService.listInstances()).willReturn(List.of());
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        String body = """
                {
                  "username": "admin",
                  "password": "test-pass"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void listInstancesWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/instances"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listInstancesWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/instances")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listInstancesWithValidTokenIsOk() throws Exception {
        String token = tokenService.issueToken(userStore.findByUsername("admin").orElseThrow()).token();

        mockMvc.perform(get("/api/instances")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void createTemplateWithViewerRoleIsForbidden() throws Exception {
        String token = tokenService.issueToken(userStore.findByUsername("viewer").orElseThrow()).token();

        String body = """
                {
                  "name": "t1",
                  "description": "Template",
                  "type": "CUSTOM",
                  "createdBy": "00000000-0000-0000-0000-000000000000"
                }
                """;

        mockMvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
