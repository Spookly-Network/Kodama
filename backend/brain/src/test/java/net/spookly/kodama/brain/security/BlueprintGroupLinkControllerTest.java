package net.spookly.kodama.brain.security;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.MethodSecurityConfig;
import net.spookly.kodama.brain.config.SecurityConfig;
import net.spookly.kodama.brain.controller.ApiExceptionHandler;
import net.spookly.kodama.brain.controller.BlueprintGroupLinkController;
import net.spookly.kodama.brain.dto.InstanceGroupDto;
import net.spookly.kodama.brain.service.BlueprintGroupLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = BlueprintGroupLinkController.class)
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
class BlueprintGroupLinkControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BlueprintGroupLinkService blueprintGroupLinkService;

  @Test
  void listGroupsReturnsLinkedGroups() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001551");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001552");
    InstanceGroupDto group =
        new InstanceGroupDto(
            groupId,
            "group-one",
            "primary",
            OffsetDateTime.parse("2025-01-01T00:00:00Z"),
            OffsetDateTime.parse("2025-01-01T00:00:00Z"));
    given(blueprintGroupLinkService.listGroupLinks(blueprintId)).willReturn(List.of(group));

    mockMvc
        .perform(get("/api/blueprints/{id}/groups", blueprintId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(groupId.toString()))
        .andExpect(jsonPath("$[0].name").value("group-one"));
  }

  @Test
  void addGroupLinkReturnsNoContent() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001553");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001554");

    mockMvc
        .perform(put("/api/blueprints/{id}/groups/{groupId}", blueprintId, groupId))
        .andExpect(status().isNoContent());

    verify(blueprintGroupLinkService).addGroupLink(blueprintId, groupId);
  }

  @Test
  void removeGroupLinkReturnsNoContent() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001555");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001556");

    mockMvc
        .perform(delete("/api/blueprints/{id}/groups/{groupId}", blueprintId, groupId))
        .andExpect(status().isNoContent());

    verify(blueprintGroupLinkService).removeGroupLink(blueprintId, groupId);
  }

  @Test
  void addGroupLinkReturnsNotFoundForMissingGroup() throws Exception {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000001557");
    UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000001558");
    willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"))
        .given(blueprintGroupLinkService)
        .addGroupLink(blueprintId, groupId);

    mockMvc
        .perform(put("/api/blueprints/{id}/groups/{groupId}", blueprintId, groupId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Group not found"));
  }
}
