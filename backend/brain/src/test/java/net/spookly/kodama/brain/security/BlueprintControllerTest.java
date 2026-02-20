package net.spookly.kodama.brain.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.ApiExceptionHandler;
import net.spookly.kodama.brain.controller.BlueprintController;
import net.spookly.kodama.brain.dto.BlueprintDto;
import net.spookly.kodama.brain.service.BlueprintService;
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

@WebMvcTest(controllers = BlueprintController.class)
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
class BlueprintControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BlueprintService blueprintService;

  @Test
  void createBlueprintReturnsCreatedStatusAndPayload() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001501");
    given(blueprintService.createBlueprint(any()))
        .willReturn(sampleDto(blueprintId, "bp-created", List.of("./run.sh")));

    String body =
        """
                {
                  "name": "bp-created",
                  "permanent": false,
                  "slotsRequired": 1,
                  "containerImage": "ghcr.io/spookly/hytale:latest",
                  "startCommand": ["./run.sh"]
                }
                """;

    mockMvc
        .perform(post("/api/blueprints").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(blueprintId.toString()))
        .andExpect(jsonPath("$.name").value("bp-created"))
        .andExpect(jsonPath("$.containerImage").value("ghcr.io/spookly/hytale:latest"))
        .andExpect(jsonPath("$.startCommand[0]").value("./run.sh"));
  }

  @Test
  void updateBlueprintReturnsOkStatusAndPayload() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001502");
    given(blueprintService.updateBlueprint(eq(blueprintId), any()))
        .willReturn(sampleDto(blueprintId, "bp-updated", List.of("java", "-jar", "server.jar")));

    String body =
        """
                {
                  "name": "bp-updated",
                  "permanent": true,
                  "slotsRequired": 2,
                  "containerImage": "ghcr.io/spookly/hytale:v2",
                  "startCommand": ["java", "-jar", "server.jar"]
                }
                """;

    mockMvc
        .perform(
            put("/api/blueprints/{id}", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(blueprintId.toString()))
        .andExpect(jsonPath("$.name").value("bp-updated"))
        .andExpect(jsonPath("$.startCommand[0]").value("java"));
  }

  @Test
  void deleteBlueprintReturnsNoContent() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001503");

    mockMvc.perform(delete("/api/blueprints/{id}", blueprintId)).andExpect(status().isNoContent());

    verify(blueprintService).deleteBlueprint(blueprintId);
  }

  @Test
  void duplicateNameReturnsConflict() throws Exception {
    given(blueprintService.createBlueprint(any()))
        .willThrow(
            new ResponseStatusException(
                HttpStatus.CONFLICT, "Blueprint with the same name already exists"));

    String body =
        """
                {
                  "name": "bp-duplicate",
                  "permanent": false,
                  "slotsRequired": 1,
                  "containerImage": "ghcr.io/spookly/hytale:latest",
                  "startCommand": ["./run.sh"]
                }
                """;

    mockMvc
        .perform(post("/api/blueprints").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Blueprint with the same name already exists"));
  }

  @Test
  void invalidFieldsReturnBadRequestWithClearErrors() throws Exception {
    String body =
        """
                {
                  "name": " ",
                  "slotsRequired": 0,
                  "containerImage": "",
                  "startCommand": []
                }
                """;

    mockMvc
        .perform(post("/api/blueprints").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("name")))
        .andExpect(jsonPath("$.message", containsString("slotsRequired")))
        .andExpect(jsonPath("$.message", containsString("containerImage")))
        .andExpect(jsonPath("$.message", containsString("startCommand")));

    verifyNoInteractions(blueprintService);
  }

  private BlueprintDto sampleDto(UUID id, String name, List<String> startCommand) {
    OffsetDateTime now = OffsetDateTime.of(2025, 2, 11, 10, 4, 53, 0, ZoneOffset.UTC);
    return new BlueprintDto(
        id,
        name,
        false,
        1,
        "ghcr.io/spookly/hytale:latest",
        null,
        startCommand,
        null,
        null,
        now,
        now);
  }
}
