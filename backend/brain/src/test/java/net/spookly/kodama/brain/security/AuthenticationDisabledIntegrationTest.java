package net.spookly.kodama.brain.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.InstanceController;
import net.spookly.kodama.brain.service.InstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InstanceController.class)
@EnableConfigurationProperties(BrainSecurityProperties.class)
@Import({
        SecurityConfig.class,
        MethodSecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class,
        ConfiguredUserStore.class,
        TestSecurityBootstrapConfig.class
})
@TestPropertySource(properties = "brain.security.enabled=false")
class AuthenticationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstanceService instanceService;

    @BeforeEach
    void setUp() {
        given(instanceService.listInstances()).willReturn(List.of());
    }

    @Test
    void listInstancesIsAccessibleWhenSecurityDisabled() throws Exception {
        mockMvc.perform(get("/api/instances"))
                .andExpect(status().isOk());
    }
}
//codex resume 019b9e4b-3f3a-70e1-9ebe-05aad51ffc22
