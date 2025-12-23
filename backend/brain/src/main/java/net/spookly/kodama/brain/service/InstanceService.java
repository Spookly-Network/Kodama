package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.dto.InstanceTemplateLayerRequest;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateLayerRepository;
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
    private final InstanceTemplateLayerRepository instanceTemplateLayerRepository;
    private final InstanceEventRepository instanceEventRepository;
    private final ObjectMapper objectMapper;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final NodeRepository nodeRepository;

    public InstanceService(
            InstanceRepository instanceRepository,
            InstanceTemplateLayerRepository instanceTemplateLayerRepository,
            InstanceEventRepository instanceEventRepository,
            ObjectMapper objectMapper,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            NodeRepository nodeRepository
    ) {
        this.instanceRepository = instanceRepository;
        this.instanceTemplateLayerRepository = instanceTemplateLayerRepository;
        this.instanceEventRepository = instanceEventRepository;
        this.objectMapper = objectMapper;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.nodeRepository = nodeRepository;
    }

    @Transactional(readOnly = true)
    public List<InstanceDto> listInstances() {
        List<Instance> instances = instanceRepository.findAll();
        Map<UUID, List<InstanceTemplateLayer>> layersByInstance = findLayers(instances);

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
        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceId(id);
        return InstanceDto.fromEntity(instance, layers);
    }

    public InstanceDto createInstance(CreateInstanceRequest request) {
        instanceRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance with the same name already exists");
        });

        List<LayerDescriptor> layerDescriptors = validateAndNormalizeTemplateLayers(request.getTemplateLayers());
        Map<LayerDescriptor, TemplateVersion> templateVersions = loadTemplateVersions(layerDescriptors);

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
        List<InstanceTemplateLayer> layers = buildLayers(savedInstance, layerDescriptors, templateVersions);
        instanceTemplateLayerRepository.saveAll(layers);

        InstanceEvent requestedEvent = new InstanceEvent(savedInstance, now, InstanceEventType.REQUEST_RECEIVED, null);
        instanceEventRepository.save(requestedEvent);

        return InstanceDto.fromEntity(savedInstance, layers);
    }

    public void reportInstancePrepared(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instance.markPrepared(now);
        instanceEventRepository.save(new InstanceEvent(instance, now, InstanceEventType.PREPARE_COMPLETED, null));
    }

    public void reportInstanceRunning(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instance.markRunning(now);
        instanceEventRepository.save(new InstanceEvent(instance, now, InstanceEventType.START_COMPLETED, null));
    }

    public void reportInstanceStopped(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instance.markStopped(now);
        instanceEventRepository.save(new InstanceEvent(instance, now, InstanceEventType.STOP_COMPLETED, null));
    }

    public void reportInstanceFailed(UUID nodeId, UUID instanceId) {
        Instance instance = loadInstanceForNode(nodeId, instanceId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        instance.markFailed(now, null);
        instanceEventRepository.save(new InstanceEvent(instance, now, InstanceEventType.FAILURE_REPORTED, null));
    }

    private Map<UUID, List<InstanceTemplateLayer>> findLayers(List<Instance> instances) {
        if (instances.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> instanceIds = instances.stream().map(Instance::getId).toList();
        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceIds(instanceIds);

        return layers.stream().collect(Collectors.groupingBy(layer -> layer.getInstance().getId()));
    }

    private List<LayerDescriptor> validateAndNormalizeTemplateLayers(List<InstanceTemplateLayerRequest> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one template layer is required");
        }

        Set<Integer> orderIndexes = new HashSet<>();
        List<LayerDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            InstanceTemplateLayerRequest layer = layers.get(i);
            if (layer.getTemplateId() == null && layer.getTemplateVersionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "templateId or templateVersionId is required for each layer");
            }

            int orderIndex = layer.getOrderIndex() != null ? layer.getOrderIndex() : i;
            if (orderIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Layer order index must be >= 0");
            }
            if (!orderIndexes.add(orderIndex)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate layer order index");
            }

            descriptors.add(new LayerDescriptor(layer.getTemplateVersionId(), layer.getTemplateId(), orderIndex));
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

    private List<InstanceTemplateLayer> buildLayers(
            Instance instance,
            List<LayerDescriptor> layerDescriptors,
            Map<LayerDescriptor, TemplateVersion> templateVersions
    ) {
        return layerDescriptors.stream()
                .sorted((a, b) -> Integer.compare(a.orderIndex(), b.orderIndex()))
                .map(descriptor -> new InstanceTemplateLayer(
                        instance,
                        templateVersions.get(descriptor),
                        descriptor.orderIndex()
                ))
                .toList();
    }

    private Map<LayerDescriptor, TemplateVersion> loadTemplateVersions(List<LayerDescriptor> layerDescriptors) {
        Map<LayerDescriptor, TemplateVersion> resolved = new HashMap<>();

        Set<UUID> templateVersionIds = layerDescriptors.stream()
                .map(LayerDescriptor::templateVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TemplateVersion> versionsById = templateVersionRepository.findAllById(templateVersionIds).stream()
                .collect(Collectors.toMap(TemplateVersion::getId, v -> v));

        if (versionsById.size() != templateVersionIds.size()) {
            Set<UUID> missingIds = new HashSet<>(templateVersionIds);
            missingIds.removeAll(versionsById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template version not found" + (missing == null ? "" : ": " + missing));
        }

        Set<UUID> templateIds = layerDescriptors.stream()
                .filter(descriptor -> descriptor.templateVersionId() == null)
                .map(LayerDescriptor::templateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Template> templatesById = templateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(Template::getId, t -> t));

        if (templatesById.size() != templateIds.size()) {
            Set<UUID> missingIds = new HashSet<>(templateIds);
            missingIds.removeAll(templatesById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template not found" + (missing == null ? "" : ": " + missing));
        }

        Map<UUID, TemplateVersion> latestVersionsByTemplate = new HashMap<>();
        for (UUID templateId : templateIds) {
            Template template = templatesById.get(templateId);
            TemplateVersion latest = templateVersionRepository.findFirstByTemplateOrderByCreatedAtDesc(template)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Template has no versions: " + templateId));
            latestVersionsByTemplate.put(templateId, latest);
        }

        for (LayerDescriptor descriptor : layerDescriptors) {
            TemplateVersion version;
            if (descriptor.templateVersionId() != null) {
                version = versionsById.get(descriptor.templateVersionId());
                if (descriptor.templateId() != null
                        && !descriptor.templateId().equals(version.getTemplate().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "templateVersionId does not belong to templateId");
                }
            } else {
                version = latestVersionsByTemplate.get(descriptor.templateId());
            }

            resolved.put(descriptor, version);
        }

        return resolved;
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

    private record LayerDescriptor(UUID templateVersionId, UUID templateId, int orderIndex) {
    }
}
