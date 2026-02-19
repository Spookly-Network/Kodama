package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.dto.BlueprintDto;
import net.spookly.kodama.brain.dto.CreateBlueprintRequest;
import net.spookly.kodama.brain.dto.UpdateBlueprintRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BlueprintService {

    private static final String BLUEPRINT_DUPLICATE_NAME_MESSAGE = "Blueprint with the same name already exists";
    private static final String BLUEPRINT_NAME_UNIQUE_CONSTRAINT = "uq_blueprints_name";
    private static final TypeReference<List<String>> START_COMMAND_TYPE = new TypeReference<>() {
    };

    private final BlueprintRepository blueprintRepository;
    private final ObjectMapper objectMapper;

    public BlueprintService(BlueprintRepository blueprintRepository, ObjectMapper objectMapper) {
        this.blueprintRepository = blueprintRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BlueprintDto> listBlueprints() {
        return blueprintRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlueprintDto getBlueprint(@NonNull UUID id) {
        Blueprint blueprint = loadActiveBlueprint(id);
        return toDto(blueprint);
    }

    public BlueprintDto createBlueprint(@NonNull CreateBlueprintRequest request) {
        rejectDuplicateName(request.getName(), null);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Blueprint blueprint = new Blueprint(
                request.getName(),
                Boolean.TRUE.equals(request.getPermanent()),
                request.getSlotsRequired(),
                request.getContainerImage(),
                request.getInstallScript(),
                serializeStartCommand(request.getStartCommand()),
                request.getVariablesJson(),
                now,
                now
        );

        Blueprint saved = saveAndFlush(blueprint);
        return toDto(saved);
    }

    public BlueprintDto updateBlueprint(@NonNull UUID id, @NonNull UpdateBlueprintRequest request) {
        Blueprint blueprint = loadActiveBlueprint(id);
        rejectDuplicateName(request.getName(), id);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        blueprint.update(
                request.getName(),
                Boolean.TRUE.equals(request.getPermanent()),
                request.getSlotsRequired(),
                request.getContainerImage(),
                request.getInstallScript(),
                serializeStartCommand(request.getStartCommand()),
                request.getVariablesJson(),
                now
        );

        flushPendingChanges();
        return toDto(blueprint);
    }

    public void deleteBlueprint(@NonNull UUID id) {
        Blueprint blueprint = loadActiveBlueprint(id);
        blueprint.softDelete(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public Blueprint loadActiveBlueprint(@NonNull UUID id) {
        return blueprintRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
    }

    @Transactional(readOnly = true)
    public Blueprint loadBlueprintForInstanceCreation(@NonNull UUID id) {
        Blueprint blueprint = blueprintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
        if (blueprint.getDeletedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Blueprint is deleted and cannot be used for new instance creation"
            );
        }
        return blueprint;
    }

    private Blueprint saveAndFlush(Blueprint blueprint) {
        try {
            return blueprintRepository.saveAndFlush(blueprint);
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteConstraintViolation(ex);
        }
    }

    private void flushPendingChanges() {
        try {
            blueprintRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteConstraintViolation(ex);
        }
    }

    private RuntimeException translateWriteConstraintViolation(DataIntegrityViolationException ex) {
        if (isDuplicateBlueprintNameViolation(ex)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, BLUEPRINT_DUPLICATE_NAME_MESSAGE, ex);
        }
        return ex;
    }

    private boolean isDuplicateBlueprintNameViolation(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT).contains(BLUEPRINT_NAME_UNIQUE_CONSTRAINT)) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(BLUEPRINT_NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    private void rejectDuplicateName(String name, UUID currentBlueprintId) {
        blueprintRepository.findByName(name).ifPresent(existing -> {
            if (currentBlueprintId == null || !existing.getId().equals(currentBlueprintId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, BLUEPRINT_DUPLICATE_NAME_MESSAGE);
            }
        });
    }

    private BlueprintDto toDto(Blueprint blueprint) {
        List<String> startCommand;
        try {
            startCommand = objectMapper.readValue(blueprint.getStartCommandJson(), START_COMMAND_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored blueprint startCommandJson is invalid", ex);
        }

        if (startCommand == null || startCommand.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored blueprint startCommandJson is invalid");
        }

        return BlueprintDto.fromEntity(blueprint, startCommand);
    }

    private String serializeStartCommand(List<String> startCommand) {
        try {
            return objectMapper.writeValueAsString(startCommand);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize startCommand", ex);
        }
    }
}
