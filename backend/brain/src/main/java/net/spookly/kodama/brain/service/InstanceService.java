package net.spookly.kodama.brain.service;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateLayer;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.dto.InstanceTemplateLayerRequest;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateLayerRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
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
    private final TemplateVersionRepository templateVersionRepository;
    private final NodeRepository nodeRepository;

    public InstanceService(
            InstanceRepository instanceRepository,
            InstanceTemplateLayerRepository instanceTemplateLayerRepository,
            InstanceEventRepository instanceEventRepository,
            TemplateVersionRepository templateVersionRepository,
            NodeRepository nodeRepository
    ) {
        this.instanceRepository = instanceRepository;
        this.instanceTemplateLayerRepository = instanceTemplateLayerRepository;
        this.instanceEventRepository = instanceEventRepository;
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

        validateTemplateLayers(request.getTemplateLayers());
        Map<UUID, TemplateVersion> templateVersions = loadTemplateVersions(request.getTemplateLayers());

        Node node = null;
        if (request.getNodeId() != null) {
            node = nodeRepository.findById(request.getNodeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instance instance = new Instance(
                request.getName(),
                request.getDisplayName(),
                InstanceState.REQUESTED,
                request.getRequestedBy(),
                node,
                request.getPortsJson(),
                request.getVariablesJson(),
                now,
                now
        );

        Instance savedInstance = instanceRepository.save(instance);
        List<InstanceTemplateLayer> layers = buildLayers(savedInstance, request.getTemplateLayers(), templateVersions);
        instanceTemplateLayerRepository.saveAll(layers);

        InstanceEvent requestedEvent = new InstanceEvent(savedInstance, now, InstanceEventType.REQUEST_RECEIVED, null);
        instanceEventRepository.save(requestedEvent);

        return InstanceDto.fromEntity(savedInstance, layers);
    }

    private Map<UUID, TemplateVersion> loadTemplateVersions(List<InstanceTemplateLayerRequest> layerRequests) {
        Set<UUID> templateVersionIds = layerRequests.stream()
                .map(InstanceTemplateLayerRequest::getTemplateVersionId)
                .collect(Collectors.toSet());

        List<TemplateVersion> versions = templateVersionRepository.findAllById(templateVersionIds);
        if (versions.size() != templateVersionIds.size()) {
            Set<UUID> foundIds = versions.stream().map(TemplateVersion::getId).collect(Collectors.toSet());
            UUID missing = templateVersionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template version not found" + (missing == null ? "" : ": " + missing));
        }

        Map<UUID, TemplateVersion> versionsById = new HashMap<>();
        versions.forEach(version -> versionsById.put(version.getId(), version));
        return versionsById;
    }

    private void validateTemplateLayers(List<InstanceTemplateLayerRequest> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one template layer is required");
        }

        Set<Integer> orderIndexes = new HashSet<>();
        for (InstanceTemplateLayerRequest layer : layers) {
            if (!orderIndexes.add(layer.getOrderIndex())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate layer order index");
            }
        }
    }

    private List<InstanceTemplateLayer> buildLayers(
            Instance instance,
            List<InstanceTemplateLayerRequest> layerRequests,
            Map<UUID, TemplateVersion> templateVersions
    ) {
        return layerRequests.stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(layerRequest -> new InstanceTemplateLayer(
                        instance,
                        templateVersions.get(layerRequest.getTemplateVersionId()),
                        layerRequest.getOrderIndex()
                ))
                .toList();
    }

    private Map<UUID, List<InstanceTemplateLayer>> findLayers(List<Instance> instances) {
        if (instances.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> instanceIds = instances.stream().map(Instance::getId).toList();
        List<InstanceTemplateLayer> layers = instanceTemplateLayerRepository.findAllByInstanceIds(instanceIds);

        return layers.stream().collect(Collectors.groupingBy(layer -> layer.getInstance().getId()));
    }
}
