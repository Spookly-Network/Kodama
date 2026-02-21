package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
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

  @Mock private TemplateService templateService;

  @Mock private InstanceGroupService instanceGroupService;

  @Mock private BlueprintService blueprintService;

  @Mock private TemplateAssignmentService templateAssignmentService;

  @Mock private BlueprintPortDefinitionService blueprintPortDefinitionService;

  @Mock private BlueprintGroupLinkService blueprintGroupLinkService;

  private InstanceCreationPreparationService instanceCreationPreparationService;

  @BeforeEach
  void setUp() {
    instanceCreationPreparationService =
        new InstanceCreationPreparationService(
            new ObjectMapper(),
            templateService,
            instanceGroupService,
            blueprintService,
            templateAssignmentService,
            blueprintPortDefinitionService,
            blueprintGroupLinkService);
  }

  @Test
  void prepareForCreateRejectsBlueprintWithoutTemplateLayers() {
    UUID blueprintId = UUID.fromString("00000000-0000-0000-0000-000000002001");

    Blueprint blueprint =
        new Blueprint(
            "blueprint-empty-layers",
            false,
            1,
            "ghcr.io/example/hytale:latest",
            null,
            "[\"./start-server.sh\"]",
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC));
    ReflectionTestUtils.setField(blueprint, "id", blueprintId);

    CreateInstanceRequest request =
        new CreateInstanceRequest("instance-empty-blueprint-layers", null);
    request.setBlueprintId(blueprintId);

    when(blueprintService.loadBlueprintForInstanceCreation(blueprintId)).thenReturn(blueprint);
    when(templateAssignmentService.listBlueprintAssignmentReferences(blueprintId))
        .thenReturn(List.of());

    assertThatThrownBy(() -> instanceCreationPreparationService.prepareForCreate(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) ex;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason())
                  .isEqualTo("template layers are required");
            });
  }

  @Test
  void prepareForCreateRejectsWhenTemplateLayersAreMissingWithoutBlueprint() {
    CreateInstanceRequest request = new CreateInstanceRequest("instance-missing-layers", null);

    assertThatThrownBy(() -> instanceCreationPreparationService.prepareForCreate(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) ex;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason())
                  .isEqualTo("template layers are required");
            });
  }
}
