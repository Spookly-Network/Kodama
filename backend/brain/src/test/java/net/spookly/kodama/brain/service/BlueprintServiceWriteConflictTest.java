package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.dto.CreateBlueprintRequest;
import net.spookly.kodama.brain.dto.UpdateBlueprintRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BlueprintServiceWriteConflictTest {

    private static final String DUPLICATE_CONSTRAINT = "uq_blueprints_name";

    @Mock
    private BlueprintRepository blueprintRepository;

    private BlueprintService blueprintService;

    @BeforeEach
    void setUp() {
        blueprintService = new BlueprintService(blueprintRepository, new ObjectMapper());
    }

    @Test
    void createBlueprintTranslatesDuplicateConstraintViolationToConflict() {
        CreateBlueprintRequest request = createRequest("bp-race");
        when(blueprintRepository.findByName("bp-race")).thenReturn(Optional.empty());
        when(blueprintRepository.saveAndFlush(any(Blueprint.class))).thenThrow(duplicateNameViolation());

        assertThatThrownBy(() -> blueprintService.createBlueprint(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusException.getReason())
                            .isEqualTo("Blueprint with the same name already exists");
                });
    }

    @Test
    void updateBlueprintTranslatesDuplicateConstraintViolationToConflict() {
        UUID blueprintId = UUID.randomUUID();
        when(blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId))
                .thenReturn(Optional.of(existingBlueprint("bp-original")));
        when(blueprintRepository.findByName("bp-race")).thenReturn(Optional.empty());
        doThrow(duplicateNameViolation()).when(blueprintRepository).flush();

        assertThatThrownBy(() -> blueprintService.updateBlueprint(blueprintId, new UpdateBlueprintRequest(
                "bp-race",
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./run.sh"),
                null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusException.getReason())
                            .isEqualTo("Blueprint with the same name already exists");
                });
    }

    @Test
    void createBlueprintKeepsNonNameIntegrityViolationAsServerError() {
        CreateBlueprintRequest request = createRequest("bp-race");
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "write failed",
                new ConstraintViolationException(
                        "could not execute statement",
                        new SQLIntegrityConstraintViolationException("Duplicate entry for key 'some_other_constraint'"),
                        "some_other_constraint"
                )
        );

        when(blueprintRepository.findByName("bp-race")).thenReturn(Optional.empty());
        when(blueprintRepository.saveAndFlush(any(Blueprint.class))).thenThrow(failure);

        assertThatThrownBy(() -> blueprintService.createBlueprint(request))
                .isSameAs(failure);
    }

    private CreateBlueprintRequest createRequest(String name) {
        return new CreateBlueprintRequest(
                name,
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                List.of("./run.sh"),
                null
        );
    }

    private Blueprint existingBlueprint(String name) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Blueprint(
                name,
                false,
                1,
                "ghcr.io/spookly/hytale:latest",
                null,
                "[\"./run.sh\"]",
                null,
                now,
                now
        );
    }

    private DataIntegrityViolationException duplicateNameViolation() {
        return new DataIntegrityViolationException(
                "write failed",
                new ConstraintViolationException(
                        "could not execute statement",
                        new SQLIntegrityConstraintViolationException(
                                "Duplicate entry 'bp-race' for key 'blueprints." + DUPLICATE_CONSTRAINT + "'"
                        ),
                        DUPLICATE_CONSTRAINT
                )
        );
    }
}
