package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.dto.BlueprintDto;
import net.spookly.kodama.brain.dto.CreateBlueprintRequest;
import net.spookly.kodama.brain.dto.UpdateBlueprintRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BlueprintService.class, BlueprintServiceTest.ObjectMapperTestConfig.class})
class BlueprintServiceTest {

  @Container private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
  }

  @TestConfiguration
  static class ObjectMapperTestConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @Autowired private BlueprintService blueprintService;

  @Autowired private BlueprintRepository blueprintRepository;

  @Test
  void createBlueprintPersistsEntityAndDefaultsSlotsRequired() {
    BlueprintDto created =
        blueprintService.createBlueprint(
            new CreateBlueprintRequest(
                "hytale-default",
                null,
                null,
                "ghcr.io/spookly/hytale:latest",
                "echo install",
                List.of("./run.sh"),
                "{\"JAVA_OPTS\":\"-Xmx2G\"}"));

    Blueprint persisted = blueprintRepository.findById(created.getId()).orElseThrow();

    assertThat(created.getSlotsRequired()).isEqualTo(1);
    assertThat(created.isPermanent()).isFalse();
    assertThat(created.getStartCommand()).containsExactly("./run.sh");
    assertThat(persisted.getSlotsRequired()).isEqualTo(1);
    assertThat(persisted.getStartCommandJson()).isEqualTo("[\"./run.sh\"]");
  }

  @Test
  void updateBlueprintUpdatesPersistedFields() {
    BlueprintDto created =
        blueprintService.createBlueprint(
            new CreateBlueprintRequest(
                "bp-update",
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./start.sh"),
                null));

    BlueprintDto updated =
        blueprintService.updateBlueprint(
            created.getId(),
            new UpdateBlueprintRequest(
                "bp-update-v2",
                true,
                3,
                "ghcr.io/spookly/hytale:v2",
                "echo preparing",
                List.of("java", "-jar", "server.jar"),
                "{\"ENV\":\"prod\"}"));

    Blueprint persisted = blueprintRepository.findById(updated.getId()).orElseThrow();

    assertThat(updated.getName()).isEqualTo("bp-update-v2");
    assertThat(updated.isPermanent()).isTrue();
    assertThat(updated.getSlotsRequired()).isEqualTo(3);
    assertThat(updated.getContainerImage()).isEqualTo("ghcr.io/spookly/hytale:v2");
    assertThat(updated.getStartCommand()).containsExactly("java", "-jar", "server.jar");
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());

    assertThat(persisted.getName()).isEqualTo("bp-update-v2");
    assertThat(persisted.getStartCommandJson()).isEqualTo("[\"java\",\"-jar\",\"server.jar\"]");
    assertThat(persisted.getDeletedAt()).isNull();
  }

  @Test
  void deleteBlueprintSetsDeletedAtAndExcludesFromList() {
    BlueprintDto created =
        blueprintService.createBlueprint(
            new CreateBlueprintRequest(
                "bp-delete",
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./run.sh"),
                null));

    blueprintService.deleteBlueprint(created.getId());

    Blueprint persisted = blueprintRepository.findById(created.getId()).orElseThrow();

    assertThat(persisted.getDeletedAt()).isNotNull();
    assertThat(blueprintService.listBlueprints()).isEmpty();
    assertThatThrownBy(() -> blueprintService.getBlueprint(created.getId()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createBlueprintRejectsDuplicateName() {
    CreateBlueprintRequest request =
        new CreateBlueprintRequest(
            "bp-duplicate",
            false,
            1,
            "ghcr.io/spookly/hytale:latest",
            null,
            List.of("./run.sh"),
            null);

    blueprintService.createBlueprint(request);

    assertThatThrownBy(() -> blueprintService.createBlueprint(request))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void updateBlueprintRejectsDuplicateName() {
    BlueprintDto first =
        blueprintService.createBlueprint(
            new CreateBlueprintRequest(
                "bp-one",
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./run.sh"),
                null));
    BlueprintDto second =
        blueprintService.createBlueprint(
            new CreateBlueprintRequest(
                "bp-two",
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./run.sh"),
                null));

    assertThatThrownBy(
            () ->
                blueprintService.updateBlueprint(
                    second.getId(),
                    new UpdateBlueprintRequest(
                        first.getName(),
                        false,
                        1,
                        "ghcr.io/spookly/hytale:latest",
                        null,
                        List.of("./run.sh"),
                        null)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }
}
