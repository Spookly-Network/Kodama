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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InstanceController.class)
@EnableConfigurationProperties(BrainSecurityProperties.class)
@Import({
        SecurityConfig.class,
        MethodSecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class,
        ConfiguredUserStore.class
})
@TestPropertySource(properties = "brain.security.enabled=false")
class AuthenticationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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
