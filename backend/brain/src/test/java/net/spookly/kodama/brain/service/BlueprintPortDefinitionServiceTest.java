package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionDto;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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
@Import(BlueprintPortDefinitionService.class)
class BlueprintPortDefinitionServiceTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private BlueprintPortDefinitionService blueprintPortDefinitionService;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Test
    void addPortDefinitionPersistsAndListsDefinitions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("bp-port-list", now);

        BlueprintPortDefinitionDto created = blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("game", "tcp", 25565, 30000, 30100, 1)
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("game");
        assertThat(created.getProtocol()).isEqualTo("tcp");
        assertThat(created.getContainerPort()).isEqualTo(25565);
        assertThat(created.getHostRange().getMin()).isEqualTo(30000);
        assertThat(created.getHostRange().getMax()).isEqualTo(30100);
        assertThat(created.getHostRange().getStep()).isEqualTo(1);

        List<BlueprintPortDefinitionDto> persisted =
                blueprintPortDefinitionService.listPortDefinitions(blueprint.getId());
        assertThat(persisted).hasSize(1);
        assertThat(persisted.getFirst().getId()).isEqualTo(created.getId());
    }

    @Test
    void addPortDefinitionRejectsDuplicateNamePerBlueprint() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("bp-port-duplicate", now);

        blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("game", "tcp", 25565, 30000, 30100, 1)
        );

        assertThatThrownBy(() -> blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("GAME", "udp", 25566, 30101, 30200, 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason())
                            .isEqualTo("Port definition name already exists for blueprint");
                });
    }

    @Test
    void addPortDefinitionAllowsSameNameOnDifferentBlueprints() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint firstBlueprint = createBlueprint("bp-port-same-name-a", now);
        Blueprint secondBlueprint = createBlueprint("bp-port-same-name-b", now);

        BlueprintPortDefinitionDto first = blueprintPortDefinitionService.addPortDefinition(
                firstBlueprint.getId(),
                request("query", "tcp", 25565, 31000, 31100, 1)
        );
        BlueprintPortDefinitionDto second = blueprintPortDefinitionService.addPortDefinition(
                secondBlueprint.getId(),
                request("query", "udp", 25566, 32000, 32100, 1)
        );

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(blueprintPortDefinitionService.listPortDefinitions(firstBlueprint.getId())).hasSize(1);
        assertThat(blueprintPortDefinitionService.listPortDefinitions(secondBlueprint.getId())).hasSize(1);
    }

    @Test
    void addPortDefinitionRejectsInvalidProtocol() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("bp-port-protocol", now);

        assertThatThrownBy(() -> blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("game", "icmp", 25565, 30000, 30100, 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addPortDefinitionRejectsInvalidHostRange() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("bp-port-range", now);

        assertThatThrownBy(() -> blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("game", "tcp", 25565, 31000, 30000, 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void removePortDefinitionDeletesDefinition() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = createBlueprint("bp-port-delete", now);

        BlueprintPortDefinitionDto created = blueprintPortDefinitionService.addPortDefinition(
                blueprint.getId(),
                request("game", "tcp", 25565, 30000, 30100, 1)
        );

        blueprintPortDefinitionService.removePortDefinition(blueprint.getId(), created.getId());

        assertThat(blueprintPortDefinitionService.listPortDefinitions(blueprint.getId())).isEmpty();
    }

    private Blueprint createBlueprint(String name, OffsetDateTime now) {
        return blueprintRepository.save(new Blueprint(
                name,
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                "[\"./run.sh\"]",
                null,
                now,
                now
        ));
    }

    private BlueprintPortDefinitionRequest request(
            String name,
            String protocol,
            int containerPort,
            int hostRangeMin,
            int hostRangeMax,
            int hostRangeStep
    ) {
        return new BlueprintPortDefinitionRequest(
                name,
                protocol,
                containerPort,
                new BlueprintPortDefinitionRequest.HostRangeRequest(hostRangeMin, hostRangeMax, hostRangeStep)
        );
    }
}
