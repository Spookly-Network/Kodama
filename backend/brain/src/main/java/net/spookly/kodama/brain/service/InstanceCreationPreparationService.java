package net.spookly.kodama.brain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.PortDefinitionRequest;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstanceCreationPreparationService {

    private final ObjectMapper objectMapper;
    private final TemplateService templateService;
    private final InstanceGroupService instanceGroupService;
    private final BlueprintService blueprintService;
    private final TemplateAssignmentService templateAssignmentService;
    private final BlueprintPortDefinitionService blueprintPortDefinitionService;
    private final BlueprintGroupLinkService blueprintGroupLinkService;

    public InstanceCreationPreparationService(
            ObjectMapper objectMapper,
            TemplateService templateService,
            InstanceGroupService instanceGroupService,
            BlueprintService blueprintService,
            TemplateAssignmentService templateAssignmentService,
            BlueprintPortDefinitionService blueprintPortDefinitionService,
            BlueprintGroupLinkService blueprintGroupLinkService
    ) {
        this.objectMapper = objectMapper;
        this.templateService = templateService;
        this.instanceGroupService = instanceGroupService;
        this.blueprintService = blueprintService;
        this.templateAssignmentService = templateAssignmentService;
        this.blueprintPortDefinitionService = blueprintPortDefinitionService;
        this.blueprintGroupLinkService = blueprintGroupLinkService;
    }

    public PreparedCreateInstanceRequest prepareForCreate(CreateInstanceRequest request) {
        Blueprint blueprint = loadBlueprintForCreate(request.getBlueprintId());
        List<AssignmentDescriptor> assignmentDescriptors = resolveAssignmentDescriptors(request, blueprint);
        AssignmentLookup assignmentLookup = resolveAssignmentReferences(assignmentDescriptors);
        List<InstanceGroup> groups = resolveEffectiveGroups(request, blueprint);

        String variablesJson = resolveVariablesJson(request, blueprint);
        RuntimeConfiguration runtimeConfiguration = resolveRuntimeConfiguration(request, blueprint);
        String portDefinitionsJson = resolvePortDefinitionsJson(request, blueprint);

        return new PreparedCreateInstanceRequest(
                blueprint,
                assignmentDescriptors,
                assignmentLookup,
                groups,
                variablesJson,
                runtimeConfiguration,
                portDefinitionsJson
        );
    }

    public List<InstanceTemplateAssignment> buildAssignments(
            Instance instance,
            PreparedCreateInstanceRequest preparedCreateRequest
    ) {
        return preparedCreateRequest.assignmentDescriptors().stream()
                .map(descriptor -> new InstanceTemplateAssignment(
                        instance,
                        preparedCreateRequest.assignmentLookup().templatesById().get(descriptor.templateId()),
                        descriptor.templateVersionId() == null
                                ? null
                                : preparedCreateRequest.assignmentLookup().versionsById().get(descriptor.templateVersionId()),
                        descriptor.priority()
                ))
                .toList();
    }

    public List<InstanceGroupMembership> buildGroupMemberships(
            Instance instance,
            PreparedCreateInstanceRequest preparedCreateRequest
    ) {
        List<InstanceGroup> groups = preparedCreateRequest.groups();
        if (groups.isEmpty()) {
            return List.of();
        }
        return groups.stream()
                .map(group -> new InstanceGroupMembership(instance, group))
                .toList();
    }

    private Blueprint loadBlueprintForCreate(UUID blueprintId) {
        if (blueprintId == null) {
            return null;
        }
        return blueprintService.loadBlueprintForInstanceCreation(blueprintId);
    }

    private List<AssignmentDescriptor> resolveAssignmentDescriptors(CreateInstanceRequest request, Blueprint blueprint) {
        if (request.getTemplateLayers() != null) {
            return validateAndNormalizeTemplateAssignments(request.getTemplateLayers(), true);
        }
        if (blueprint != null) {
            List<AssignmentDescriptor> assignmentDescriptors =
                    templateAssignmentService.listBlueprintAssignmentReferences(blueprint.getId()).stream()
                    .map(assignmentReference -> new AssignmentDescriptor(
                            assignmentReference.templateId(),
                            assignmentReference.templateVersionId(),
                            assignmentReference.priority()
                    ))
                    .toList();
            requireTemplateLayers(assignmentDescriptors);
            return assignmentDescriptors;
        }
        return validateAndNormalizeTemplateAssignments(null, true);
    }

    private List<AssignmentDescriptor> validateAndNormalizeTemplateAssignments(
            List<TemplateAssignmentRequest> assignments,
            boolean required
    ) {
        if (assignments == null || assignments.isEmpty()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "template layers are required");
            }
            return List.of();
        }

        List<AssignmentDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < assignments.size(); i++) {
            TemplateAssignmentRequest assignment = assignments.get(i);
            if (assignment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template assignment entry is required");
            }
            if (assignment.getTemplateId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required for each assignment");
            }

            int priority = assignment.getPriority() != null ? assignment.getPriority() : i;
            if (priority < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be >= 0");
            }

            descriptors.add(new AssignmentDescriptor(
                    assignment.getTemplateId(),
                    assignment.getTemplateVersionId(),
                    priority
            ));
        }
        return descriptors;
    }

    private void requireTemplateLayers(List<?> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "template layers are required");
        }
    }

    private String resolveVariablesJson(CreateInstanceRequest request, Blueprint blueprint) {
        Map<String, String> variables = request.getVariables();
        String variablesJson = request.getVariablesJson();
        if (variables != null && variablesJson != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either variables or variablesJson, not both");
        }
        if (variables != null) {
            try {
                return objectMapper.writeValueAsString(variables);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize variables", e);
            }
        }
        if (variablesJson != null) {
            return variablesJson;
        }
        return blueprint == null ? null : blueprint.getVariablesJson();
    }

    private RuntimeConfiguration resolveRuntimeConfiguration(CreateInstanceRequest request, Blueprint blueprint) {
        Boolean permanent = request.getPermanent() != null
                ? request.getPermanent()
                : blueprint == null ? null : blueprint.isPermanent();

        Integer slotsRequired = request.getSlotsRequired() != null
                ? request.getSlotsRequired()
                : blueprint == null ? null : blueprint.getSlotsRequired();

        String containerImage = request.getContainerImage() != null
                ? request.getContainerImage()
                : blueprint == null ? null : blueprint.getContainerImage();

        String installScript = request.getInstallScript() != null
                ? request.getInstallScript()
                : blueprint == null ? null : blueprint.getInstallScript();

        String startCommandJson = resolveStartCommandJson(request.getStartCommand(), blueprint);

        return new RuntimeConfiguration(
                permanent,
                slotsRequired == null ? 1 : slotsRequired,
                containerImage,
                installScript,
                startCommandJson
        );
    }

    private String resolveStartCommandJson(List<String> startCommand, Blueprint blueprint) {
        if (startCommand == null) {
            return blueprint == null ? null : blueprint.getStartCommandJson();
        }
        if (startCommand.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startCommand must not be empty");
        }
        for (String token : startCommand) {
            if (token == null || token.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startCommand entries must not be blank");
            }
        }
        try {
            return objectMapper.writeValueAsString(startCommand);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize startCommand", ex);
        }
    }

    private String resolvePortDefinitionsJson(CreateInstanceRequest request, Blueprint blueprint) {
        if (request.getPortDefinitions() != null) {
            List<PortDefinitionJson> overrideDefinitions = normalizePortDefinitions(request.getPortDefinitions());
            return serializePortDefinitions(overrideDefinitions);
        }

        if (blueprint == null) {
            return null;
        }

        List<PortDefinitionJson> blueprintDefinitions = normalizePortDefinitions(
                blueprintPortDefinitionService.listPortDefinitionRequests(blueprint.getId())
        );
        return serializePortDefinitions(blueprintDefinitions);
    }

    private List<PortDefinitionJson> normalizePortDefinitions(List<PortDefinitionRequest> definitions) {
        List<PortDefinitionJson> normalized = new ArrayList<>();
        for (PortDefinitionRequest definition : definitions) {
            if (definition == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Port definition entry is required");
            }
            PortDefinitionRequest.HostRangeRequest hostRange = definition.getHostRange();
            if (hostRange == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange is required");
            }

            Integer containerPort = definition.getContainerPort();
            if (containerPort == null || containerPort < 1 || containerPort > 65535) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "containerPort must be between 1 and 65535");
            }

            Integer min = hostRange.getMin();
            Integer max = hostRange.getMax();
            Integer step = hostRange.getStep();
            if (min == null || max == null || step == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange min/max/step are required");
            }
            if (min < 1 || min > 65535 || max < 1 || max > 65535) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange min/max must be between 1 and 65535");
            }
            if (step < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange step must be >= 1");
            }
            if (min > max) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hostRange min must be <= max");
            }

            normalized.add(new PortDefinitionJson(
                    definition.getName(),
                    normalizeProtocol(definition.getProtocol()),
                    containerPort,
                    new PortRangeJson(min, max, step)
            ));
        }
        return normalized;
    }

    private String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "protocol is required");
        }
        String normalized = protocol.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("tcp") && !normalized.equals("udp")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "protocol must be tcp or udp");
        }
        return normalized;
    }

    private String serializePortDefinitions(List<PortDefinitionJson> definitions) {
        try {
            return objectMapper.writeValueAsString(definitions);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize portDefinitions", ex);
        }
    }

    private List<InstanceGroup> resolveEffectiveGroups(CreateInstanceRequest request, Blueprint blueprint) {
        if (request.getGroupIds() != null) {
            return instanceGroupService.loadGroupsById(request.getGroupIds());
        }
        if (blueprint == null) {
            return List.of();
        }
        List<UUID> blueprintGroupIds = blueprintGroupLinkService.listLinkedGroupIds(blueprint.getId());
        return instanceGroupService.loadGroupsById(blueprintGroupIds);
    }

    private AssignmentLookup resolveAssignmentReferences(List<AssignmentDescriptor> descriptors) {
        Set<UUID> templateIds = descriptors.stream()
                .map(AssignmentDescriptor::templateId)
                .collect(Collectors.toSet());

        Map<UUID, Template> templatesById = templateService.loadTemplatesById(templateIds);

        Set<UUID> templateVersionIds = descriptors.stream()
                .map(AssignmentDescriptor::templateVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TemplateVersion> versionsById = templateService.loadTemplateVersionsById(templateVersionIds);

        for (AssignmentDescriptor descriptor : descriptors) {
            if (descriptor.templateVersionId() == null) {
                continue;
            }
            TemplateVersion version = versionsById.get(descriptor.templateVersionId());
            if (!descriptor.templateId().equals(version.getTemplate().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "templateVersionId does not belong to templateId");
            }
        }

        templateService.ensureTemplatesHaveVersions(descriptors.stream()
                .filter(descriptor -> descriptor.templateVersionId() == null)
                .map(AssignmentDescriptor::templateId)
                .collect(Collectors.toSet()));

        return new AssignmentLookup(templatesById, versionsById);
    }

    record PreparedCreateInstanceRequest(
            Blueprint blueprint,
            List<AssignmentDescriptor> assignmentDescriptors,
            AssignmentLookup assignmentLookup,
            List<InstanceGroup> groups,
            String variablesJson,
            RuntimeConfiguration runtimeConfiguration,
            String portDefinitionsJson
    ) {
    }

    record RuntimeConfiguration(
            Boolean permanent,
            Integer slotsRequired,
            String containerImage,
            String installScript,
            String startCommandJson
    ) {
    }

    record AssignmentDescriptor(UUID templateId, UUID templateVersionId, int priority) {
    }

    record AssignmentLookup(
            Map<UUID, Template> templatesById,
            Map<UUID, TemplateVersion> versionsById
    ) {
    }

    private record PortDefinitionJson(
            String name,
            String protocol,
            int containerPort,
            PortRangeJson hostRange
    ) {
    }

    private record PortRangeJson(
            int min,
            int max,
            int step
    ) {
    }
}
