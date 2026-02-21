package net.spookly.kodama.brain.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.ApiExceptionHandler;
import net.spookly.kodama.brain.controller.BlueprintPortDefinitionController;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionDto;
import net.spookly.kodama.brain.service.BlueprintPortDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = BlueprintPortDefinitionController.class)
@EnableConfigurationProperties(BrainSecurityProperties.class)
@Import({
  ApiExceptionHandler.class,
  SecurityConfig.class,
  MethodSecurityConfig.class,
  JwtAuthFilter.class,
  JwtTokenService.class,
  ConfiguredUserStore.class,
  TestSecurityBootstrapConfig.class
})
@TestPropertySource(properties = "brain.security.enabled=false")
class BlueprintPortDefinitionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BlueprintPortDefinitionService blueprintPortDefinitionService;

  @Test
  void listPortDefinitionsReturnsDefinitions() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001541");
    BlueprintPortDefinitionDto definition =
        new BlueprintPortDefinitionDto(
            UUID.fromString("00000000-0000-0000-0000-000000001542"),
            "game",
            "tcp",
            25565,
            new BlueprintPortDefinitionDto.HostRangeDto(30000, 30100, 1));
    given(blueprintPortDefinitionService.listPortDefinitions(blueprintId))
        .willReturn(List.of(definition));

    mockMvc
        .perform(get("/api/blueprints/{id}/ports", blueprintId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(definition.getId().toString()))
        .andExpect(jsonPath("$[0].name").value("game"))
        .andExpect(jsonPath("$[0].protocol").value("tcp"))
        .andExpect(jsonPath("$[0].containerPort").value(25565))
        .andExpect(jsonPath("$[0].hostRange.min").value(30000))
        .andExpect(jsonPath("$[0].hostRange.max").value(30100))
        .andExpect(jsonPath("$[0].hostRange.step").value(1));
  }

  @Test
  void addPortDefinitionReturnsCreatedStatusAndPayload() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001543");
    UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000001544");
    given(blueprintPortDefinitionService.addPortDefinition(eq(blueprintId), any()))
        .willReturn(
            new BlueprintPortDefinitionDto(
                definitionId,
                "query",
                "udp",
                25566,
                new BlueprintPortDefinitionDto.HostRangeDto(31000, 31100, 2)));

    String body =
        """
                {
                  "name": "query",
                  "protocol": "udp",
                  "containerPort": 25566,
                  "hostRange": {
                    "min": 31000,
                    "max": 31100,
                    "step": 2
                  }
                }
                """;

    mockMvc
        .perform(
            post("/api/blueprints/{id}/ports", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(definitionId.toString()))
        .andExpect(jsonPath("$.name").value("query"))
        .andExpect(jsonPath("$.protocol").value("udp"))
        .andExpect(jsonPath("$.containerPort").value(25566))
        .andExpect(jsonPath("$.hostRange.min").value(31000))
        .andExpect(jsonPath("$.hostRange.max").value(31100))
        .andExpect(jsonPath("$.hostRange.step").value(2));
  }

  @Test
  void removePortDefinitionReturnsNoContent() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001545");
    UUID portId = UUID.fromString("00000000-0000-0000-0000-000000001546");

    mockMvc
        .perform(delete("/api/blueprints/{id}/ports/{portId}", blueprintId, portId))
        .andExpect(status().isNoContent());

    verify(blueprintPortDefinitionService).removePortDefinition(blueprintId, portId);
  }

  @Test
  void invalidPayloadReturnsBadRequest() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001547");
    String body =
        """
                {
                  "protocol": "",
                  "containerPort": 0,
                  "hostRange": {}
                }
                """;

    mockMvc
        .perform(
            post("/api/blueprints/{id}/ports", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("name")))
        .andExpect(jsonPath("$.message", containsString("protocol")))
        .andExpect(jsonPath("$.message", containsString("containerPort")))
        .andExpect(jsonPath("$.message", containsString("hostRange.min")))
        .andExpect(jsonPath("$.message", containsString("hostRange.max")))
        .andExpect(jsonPath("$.message", containsString("hostRange.step")));

    verifyNoInteractions(blueprintPortDefinitionService);
  }

  @Test
  void addPortDefinitionReturnsBadRequestForInvalidProtocol() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001548");
    given(blueprintPortDefinitionService.addPortDefinition(eq(blueprintId), any()))
        .willThrow(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "protocol must be one of: tcp, udp"));

    String body =
        """
                {
                  "name": "game",
                  "protocol": "icmp",
                  "containerPort": 25565,
                  "hostRange": {
                    "min": 30000,
                    "max": 30100,
                    "step": 1
                  }
                }
                """;

    mockMvc
        .perform(
            post("/api/blueprints/{id}/ports", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("protocol must be one of: tcp, udp"));
  }
}
