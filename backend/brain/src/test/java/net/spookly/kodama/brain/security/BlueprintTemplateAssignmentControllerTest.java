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
import net.spookly.kodama.brain.controller.BlueprintTemplateAssignmentController;
import net.spookly.kodama.brain.dto.TemplateAssignmentDto;
import net.spookly.kodama.brain.service.TemplateAssignmentService;
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

@WebMvcTest(controllers = BlueprintTemplateAssignmentController.class)
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
class BlueprintTemplateAssignmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TemplateAssignmentService templateAssignmentService;

  @Test
  void listAssignmentsReturnsAssignments() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001521");
    TemplateAssignmentDto assignment =
        new TemplateAssignmentDto(
            UUID.fromString("00000000-0000-0000-0000-000000001522"),
            UUID.fromString("00000000-0000-0000-0000-000000001523"),
            UUID.fromString("00000000-0000-0000-0000-000000001524"),
            0);
    given(templateAssignmentService.listBlueprintAssignments(blueprintId))
        .willReturn(List.of(assignment));

    mockMvc
        .perform(get("/api/blueprints/{id}/template-assignments", blueprintId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(assignment.getId().toString()))
        .andExpect(jsonPath("$[0].templateId").value(assignment.getTemplateId().toString()))
        .andExpect(
            jsonPath("$[0].templateVersionId").value(assignment.getTemplateVersionId().toString()))
        .andExpect(jsonPath("$[0].priority").value(0));
  }

  @Test
  void addAssignmentReturnsCreatedStatusAndPayload() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001525");
    UUID assignmentId = UUID.fromString("00000000-0000-0000-0000-000000001526");
    UUID templateId = UUID.fromString("00000000-0000-0000-0000-000000001527");
    UUID templateVersionId = UUID.fromString("00000000-0000-0000-0000-000000001528");
    given(templateAssignmentService.addBlueprintAssignment(eq(blueprintId), any()))
        .willReturn(new TemplateAssignmentDto(assignmentId, templateId, templateVersionId, 2));

    String body =
        """
                {
                  "templateId": "%s",
                  "templateVersionId": "%s",
                  "priority": 2
                }
                """
            .formatted(templateId, templateVersionId);

    mockMvc
        .perform(
            post("/api/blueprints/{id}/template-assignments", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(assignmentId.toString()))
        .andExpect(jsonPath("$.templateId").value(templateId.toString()))
        .andExpect(jsonPath("$.templateVersionId").value(templateVersionId.toString()))
        .andExpect(jsonPath("$.priority").value(2));
  }

  @Test
  void removeAssignmentReturnsNoContent() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001529");
    UUID assignmentId = UUID.fromString("00000000-0000-0000-0000-000000001530");

    mockMvc
        .perform(
            delete(
                "/api/blueprints/{id}/template-assignments/{assignmentId}",
                blueprintId,
                assignmentId))
        .andExpect(status().isNoContent());

    verify(templateAssignmentService).removeBlueprintAssignment(blueprintId, assignmentId);
  }

  @Test
  void invalidPayloadReturnsBadRequest() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001531");
    String body =
        """
                {
                  "priority": -1
                }
                """;

    mockMvc
        .perform(
            post("/api/blueprints/{id}/template-assignments", blueprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("templateId")))
        .andExpect(jsonPath("$.message", containsString("priority")));

    verifyNoInteractions(templateAssignmentService);
  }

  @Test
  void listAssignmentsReturnsNotFoundWhenBlueprintIsMissing() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001532");
    given(templateAssignmentService.listBlueprintAssignments(blueprintId))
        .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));

    mockMvc
        .perform(get("/api/blueprints/{id}/template-assignments", blueprintId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Blueprint not found"));
  }
}
