package net.spookly.kodama.brain.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintPortDefinition;
import net.spookly.kodama.brain.domain.blueprint.PortProtocol;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionDto;
import net.spookly.kodama.brain.dto.BlueprintPortDefinitionRequest;
import net.spookly.kodama.brain.repository.BlueprintPortDefinitionRepository;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BlueprintPortDefinitionService {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65_535;
    private static final String DUPLICATE_PORT_NAME_CONSTRAINT = "uq_blueprint_port_definitions_blueprint_name";
    private static final String DUPLICATE_PORT_NAME_MESSAGE = "Port definition name already exists for blueprint";

    private final BlueprintRepository blueprintRepository;
    private final BlueprintPortDefinitionRepository blueprintPortDefinitionRepository;

    public BlueprintPortDefinitionService(
            BlueprintRepository blueprintRepository,
            BlueprintPortDefinitionRepository blueprintPortDefinitionRepository
    ) {
        this.blueprintRepository = blueprintRepository;
        this.blueprintPortDefinitionRepository = blueprintPortDefinitionRepository;
    }

    @Transactional(readOnly = true)
    public List<BlueprintPortDefinitionDto> listPortDefinitions(UUID blueprintId) {
        ensureBlueprintExists(blueprintId);
        return blueprintPortDefinitionRepository.findAllByBlueprintId(blueprintId).stream()
                .map(BlueprintPortDefinitionDto::fromEntity)
                .toList();
    }

    public BlueprintPortDefinitionDto addPortDefinition(UUID blueprintId, BlueprintPortDefinitionRequest request) {
        Blueprint blueprint = loadBlueprint(blueprintId);
        BlueprintPortDefinitionRequest safeRequest = requireRequest(request);
        BlueprintPortDefinitionRequest.HostRangeRequest hostRange = requireHostRange(safeRequest);

        String name = normalizeName(safeRequest.getName());
        PortProtocol protocol = parseProtocol(safeRequest.getProtocol());
        int containerPort = requirePort(safeRequest.getContainerPort(), "containerPort");
        int hostRangeMin = requirePort(hostRange.getMin(), "hostRange.min");
        int hostRangeMax = requirePort(hostRange.getMax(), "hostRange.max");
        int hostRangeStep = requirePositive(hostRange.getStep(), "hostRange.step");

        if (hostRangeMin > hostRangeMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange.min must be <= hostRange.max");
        }

        BlueprintPortDefinition portDefinition = new BlueprintPortDefinition(
                blueprint,
                name,
                protocol,
                containerPort,
                hostRangeMin,
                hostRangeMax,
                hostRangeStep
        );

        BlueprintPortDefinition saved = saveAndFlush(portDefinition);
        return BlueprintPortDefinitionDto.fromEntity(saved);
    }

    public void removePortDefinition(UUID blueprintId, UUID portId) {
        ensureBlueprintExists(blueprintId);
        BlueprintPortDefinition portDefinition = blueprintPortDefinitionRepository.findById(portId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port definition not found"));
        if (!portDefinition.getBlueprint().getId().equals(blueprintId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Port definition not found");
        }
        blueprintPortDefinitionRepository.delete(portDefinition);
    }

    private Blueprint loadBlueprint(UUID blueprintId) {
        return blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
    }

    private void ensureBlueprintExists(UUID blueprintId) {
        if (blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        return name.trim();
    }

    private PortProtocol parseProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "protocol is required");
        }

        String normalized = protocol.trim().toUpperCase(Locale.ROOT);
        try {
            return PortProtocol.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "protocol must be one of: tcp, udp");
        }
    }

    private BlueprintPortDefinitionRequest requireRequest(BlueprintPortDefinitionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Port definition request is required");
        }
        return request;
    }

    private BlueprintPortDefinitionRequest.HostRangeRequest requireHostRange(BlueprintPortDefinitionRequest request) {
        if (request.getHostRange() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange is required");
        }
        return request.getHostRange();
    }

    private int requirePort(Integer value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (value < MIN_PORT || value > MAX_PORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be between " + MIN_PORT + " and " + MAX_PORT);
        }
        return value;
    }

    private int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (value < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be >= 1");
        }
        return value;
    }

    private BlueprintPortDefinition saveAndFlush(BlueprintPortDefinition portDefinition) {
        try {
            return blueprintPortDefinitionRepository.saveAndFlush(portDefinition);
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteConstraintViolation(ex);
        }
    }

    private RuntimeException translateWriteConstraintViolation(DataIntegrityViolationException ex) {
        if (isDuplicatePortNameViolation(ex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DUPLICATE_PORT_NAME_MESSAGE, ex);
        }
        return ex;
    }

    private boolean isDuplicatePortNameViolation(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT).contains(DUPLICATE_PORT_NAME_CONSTRAINT)) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(DUPLICATE_PORT_NAME_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }
}
