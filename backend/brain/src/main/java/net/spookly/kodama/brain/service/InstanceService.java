package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;
    private final InstanceEventRepository instanceEventRepository;
    private final InstanceStateMachine instanceStateMachine;
    private final ObjectMapper objectMapper;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final NodeRepository nodeRepository;
    private final TemplateAssignmentResolver templateAssignmentResolver;

    public InstanceService(
            InstanceRepository instanceRepository,
            InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository,
            InstanceEventRepository instanceEventRepository,
            InstanceStateMachine instanceStateMachine,
            ObjectMapper objectMapper,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            NodeRepository nodeRepository,
            TemplateAssignmentResolver templateAssignmentResolver
    ) {
        this.instanceRepository = instanceRepository;
        this.instanceTemplateAssignmentRepository = instanceTemplateAssignmentRepository;
        this.instanceEventRepository = instanceEventRepository;
        this.instanceStateMachine = instanceStateMachine;
        this.objectMapper = objectMapper;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.nodeRepository = nodeRepository;
        this.templateAssignmentResolver = templateAssignmentResolver;
    }

    @Transactional(readOnly = true)
    public List<InstanceDto> listInstances() {
        List<Instance> instances = instanceRepository.findAll();
        Map<UUID, List<ResolvedTemplateLayer>> layersByInstance =
                templateAssignmentResolver.resolveForInstances(instances.stream().map(Instance::getId).toList());

        return instances.stream()
                .map(instance -> InstanceDto.fromEntity(
                        instance,
                        layersByInstance.getOrDefault(instance.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public InstanceDto getInstance(UUID id) {
        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
        List<ResolvedTemplateLayer> layers = templateAssignmentResolver.resolveForInstance(id);
        return InstanceDto.fromEntity(instance, layers);
    }

    public InstanceDto createInstance(CreateInstanceRequest request) {
        instanceRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance with the same name already exists");
        });

        List<AssignmentDescriptor> assignmentDescriptors =
                validateAndNormalizeTemplateAssignments(request.getTemplateLayers());
        AssignmentLookup assignmentLookup = resolveAssignmentReferences(assignmentDescriptors);

        Node node = null;
        if (request.getNodeId() != null) {
            node = nodeRepository.findById(request.getNodeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String variablesJson = resolveVariablesJson(request);
        Instance instance = new Instance(
                request.getName(),
                request.getDisplayName(),
                InstanceState.REQUESTED,
                request.getRequestedBy(),
                node,
                request.getRegion(),
                request.getTags(),
                request.getDevModeAllowed(),
                request.getPortsJson(),
                variablesJson,
                now,
                now
        );

        Instance savedInstance = instanceRepository.save(instance);
        List<InstanceTemplateAssignment> assignments =
                buildAssignments(savedInstance, assignmentDescriptors, assignmentLookup);
        instanceTemplateAssignmentRepository.saveAll(assignments);

        InstanceEvent requestedEvent = new InstanceEvent(savedInstance, now, InstanceEventType.REQUEST_RECEIVED, null);
        instanceEventRepository.save(requestedEvent);

        List<ResolvedTemplateLayer> resolvedLayers =
                templateAssignmentResolver.resolveForInstance(savedInstance.getId());
        return InstanceDto.fromEntity(savedInstance, resolvedLayers);
    }

    public void reportInstancePrepared(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instanceStateMachine.transition(instance, InstanceState.STARTING, InstanceEventType.PREPARE_COMPLETED, now);
    }

    public void reportInstanceRunning(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instanceStateMachine.transition(instance, InstanceState.RUNNING, InstanceEventType.START_COMPLETED, now);
    }

    public void reportInstanceStopped(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instanceStateMachine.transition(instance, InstanceState.STOPPED, InstanceEventType.STOP_COMPLETED, now);
    }

    public void reportInstanceDestroyed(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instanceStateMachine.transition(instance, InstanceState.DESTROYED, InstanceEventType.DESTROY_COMPLETED, now);
    }

    public void reportInstanceFailed(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instanceStateMachine.transition(instance, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED, now, null);
    }

    private List<AssignmentDescriptor> validateAndNormalizeTemplateAssignments(
            List<TemplateAssignmentRequest> assignments
    ) {
        if (assignments == null || assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one template assignment is required");
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

    private String resolveVariablesJson(CreateInstanceRequest request) {
        Map<String, String> variables = request.getVariables();
        String variablesJson = request.getVariablesJson();
        if (variables != null && variablesJson != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either variables or variablesJson, not both");
        }
        if (variables == null) {
            return variablesJson;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize variables", e);
        }
    }

    private AssignmentLookup resolveAssignmentReferences(List<AssignmentDescriptor> descriptors) {
        Set<UUID> templateIds = descriptors.stream()
                .map(AssignmentDescriptor::templateId)
                .collect(Collectors.toSet());

        Map<UUID, Template> templatesById = templateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(Template::getId, template -> template));

        if (templatesById.size() != templateIds.size()) {
            Set<UUID> missingIds = new HashSet<>(templateIds);
            missingIds.removeAll(templatesById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template not found" + (missing == null ? "" : ": " + missing));
        }

        Set<UUID> templateVersionIds = descriptors.stream()
                .map(AssignmentDescriptor::templateVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TemplateVersion> versionsById = templateVersionRepository.findAllById(templateVersionIds).stream()
                .collect(Collectors.toMap(TemplateVersion::getId, version -> version));

        if (versionsById.size() != templateVersionIds.size()) {
            Set<UUID> missingIds = new HashSet<>(templateVersionIds);
            missingIds.removeAll(versionsById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template version not found" + (missing == null ? "" : ": " + missing));
        }

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

        return new AssignmentLookup(templatesById, versionsById);
    }

    private List<InstanceTemplateAssignment> buildAssignments(
            Instance instance,
            List<AssignmentDescriptor> descriptors,
            AssignmentLookup lookup
    ) {
        return descriptors.stream()
                .map(descriptor -> new InstanceTemplateAssignment(
                        instance,
                        lookup.templatesById().get(descriptor.templateId()),
                        descriptor.templateVersionId() == null
                                ? null
                                : lookup.versionsById().get(descriptor.templateVersionId()),
                        descriptor.priority()
                ))
                .toList();
    }

    private Instance loadInstanceForNode(UUID nodeId, UUID instanceId) {
        nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
        if (instance.getNode() == null || instance.getNode().getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance is not assigned to a node");
        }
        if (!instance.getNode().getId().equals(nodeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance is not assigned to the requested node");
        }
        return instance;
    }

    private record AssignmentDescriptor(UUID templateId, UUID templateVersionId, int priority) {
    }

    private record AssignmentLookup(
            Map<UUID, Template> templatesById,
            Map<UUID, TemplateVersion> versionsById
    ) {
    }
}
