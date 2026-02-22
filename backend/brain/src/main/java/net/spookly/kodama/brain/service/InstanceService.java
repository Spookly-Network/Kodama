package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceEvent;
import net.spookly.kodama.brain.domain.instance.InstanceEventType;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceState;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.repository.InstanceEventRepository;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import net.spookly.kodama.brain.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InstanceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstanceService.class);

  private final InstanceRepository instanceRepository;
  private final InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;
  private final InstanceEventRepository instanceEventRepository;
  private final InstanceStateMachine instanceStateMachine;
  private final CommandDispatcherService commandDispatcherService;
  private final InstanceGroupMembershipRepository instanceGroupMembershipRepository;
  private final TemplateAssignmentResolver templateAssignmentResolver;
  private final InstanceCreationPreparationService instanceCreationPreparationService;
  private final InstanceNodeResolutionService instanceNodeResolutionService;

  public InstanceService(
      InstanceRepository instanceRepository,
      InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository,
      InstanceEventRepository instanceEventRepository,
      InstanceStateMachine instanceStateMachine,
      CommandDispatcherService commandDispatcherService,
      InstanceGroupMembershipRepository instanceGroupMembershipRepository,
      TemplateAssignmentResolver templateAssignmentResolver,
      InstanceCreationPreparationService instanceCreationPreparationService,
      InstanceNodeResolutionService instanceNodeResolutionService) {
    this.instanceRepository = instanceRepository;
    this.instanceTemplateAssignmentRepository = instanceTemplateAssignmentRepository;
    this.instanceEventRepository = instanceEventRepository;
    this.instanceStateMachine = instanceStateMachine;
    this.commandDispatcherService = commandDispatcherService;
    this.instanceGroupMembershipRepository = instanceGroupMembershipRepository;
    this.templateAssignmentResolver = templateAssignmentResolver;
    this.instanceCreationPreparationService = instanceCreationPreparationService;
    this.instanceNodeResolutionService = instanceNodeResolutionService;
  }

  @Transactional(readOnly = true)
  public List<InstanceDto> listInstances() {
    List<Instance> instances = instanceRepository.findAll();
    Map<UUID, List<ResolvedTemplateLayer>> layersByInstance =
        templateAssignmentResolver.resolveForInstances(
            instances.stream().map(Instance::getId).toList());

    return instances.stream()
        .map(
            instance ->
                InstanceDto.fromEntity(
                    instance, layersByInstance.getOrDefault(instance.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public InstanceDto getInstance(UUID id) {
    Instance instance =
        instanceRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
    List<ResolvedTemplateLayer> layers = templateAssignmentResolver.resolveForInstance(id);
    return InstanceDto.fromEntity(instance, layers);
  }

  public InstanceDto createInstance(CreateInstanceRequest request) {
    instanceRepository
        .findByName(request.getName())
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "Instance with the same name already exists");
            });

    InstanceCreationPreparationService.PreparedCreateInstanceRequest preparedCreateRequest =
        instanceCreationPreparationService.prepareForCreate(request);
    InstanceCreationPreparationService.RuntimeConfiguration runtimeConfiguration =
        preparedCreateRequest.runtimeConfiguration();

    Node node =
        instanceNodeResolutionService.resolveNodeForCreate(
            request, runtimeConfiguration.slotsRequired());

    OffsetDateTime now = TimeUtils.utcNow();
    Instance instance =
        new Instance(
            request.getName(),
            request.getDisplayName(),
            InstanceState.REQUESTED,
            request.getRequestedBy(),
            node,
            request.getRegion(),
            request.getTags(),
            request.getDevModeAllowed(),
            request.getPortsJson(),
            preparedCreateRequest.variablesJson(),
            now,
            now);
    instance.applyBlueprintAndRuntime(
        preparedCreateRequest.blueprint(),
        runtimeConfiguration.permanent(),
        runtimeConfiguration.slotsRequired(),
        runtimeConfiguration.containerImage(),
        runtimeConfiguration.installScript(),
        runtimeConfiguration.startCommandJson(),
        preparedCreateRequest.portDefinitionsJson());

    Instance savedInstance = instanceRepository.save(instance);
    List<InstanceTemplateAssignment> assignments =
        instanceCreationPreparationService.buildAssignments(savedInstance, preparedCreateRequest);
    instanceTemplateAssignmentRepository.saveAll(assignments);
    List<InstanceGroupMembership> memberships =
        instanceCreationPreparationService.buildGroupMemberships(
            savedInstance, preparedCreateRequest);
    instanceGroupMembershipRepository.saveAll(memberships);

    InstanceEvent requestedEvent =
        new InstanceEvent(savedInstance, now, InstanceEventType.REQUEST_RECEIVED, null);
    instanceEventRepository.save(requestedEvent);

    List<ResolvedTemplateLayer> resolvedLayers =
        templateAssignmentResolver.resolveForInstance(savedInstance.getId());
    return InstanceDto.fromEntity(savedInstance, resolvedLayers);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public InstanceDto startInstance(UUID id) {
    Instance instance = loadInstance(id);
    OffsetDateTime now = TimeUtils.utcNow();
    InstanceState state = instance.getState();
    Node node = instanceNodeResolutionService.requireAssignedNode(instance);
    LOGGER.info(
        "Start instance requested. instanceId={} state={} nodeId={} nodeBaseUrl={}",
        id,
        state,
        node.getId(),
        node.getBaseUrl());

    if (state == InstanceState.REQUESTED) {
      List<ResolvedTemplateLayer> layers = templateAssignmentResolver.resolveForInstance(id);
      LOGGER.info("Instance {} has been requested on node {}", id, node.getId());
      transitionOrConflict(
          instance, InstanceState.PREPARING, InstanceEventType.PREPARE_DISPATCHED, now);
      dispatchNodeCommand(
          "prepare",
          () -> commandDispatcherService.sendPrepareInstance(node, instance, layers, null));
    } else if (state == InstanceState.STOPPED) {
      transitionOrConflict(
          instance, InstanceState.STARTING, InstanceEventType.START_DISPATCHED, now);
      dispatchNodeCommand(
          "start", () -> commandDispatcherService.sendStartInstance(node, instance));
    } else {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Instance cannot be started from state " + state);
    }

    Instance persisted = loadInstance(id);
    return InstanceDto.fromEntity(persisted, templateAssignmentResolver.resolveForInstance(id));
  }

  public InstanceDto stopInstance(UUID id) {
    Instance instance = loadInstance(id);
    Node node = instanceNodeResolutionService.requireAssignedNode(instance);
    InstanceState state = instance.getState();
    if (state != InstanceState.RUNNING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Instance cannot be stopped from state " + state);
    }
    OffsetDateTime now = TimeUtils.utcNow();
    transitionOrConflict(instance, InstanceState.STOPPING, InstanceEventType.STOP_DISPATCHED, now);
    dispatchNodeCommand("stop", () -> commandDispatcherService.sendStopInstance(node, instance));
    return InstanceDto.fromEntity(instance, templateAssignmentResolver.resolveForInstance(id));
  }

  public InstanceDto destroyInstance(UUID id) {
    Instance instance = loadInstance(id);
    Node node = instanceNodeResolutionService.requireAssignedNode(instance);
    InstanceState state = instance.getState();
    if (state != InstanceState.STOPPED
        && state != InstanceState.STOPPING
        && state != InstanceState.FAILED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Instance cannot be destroyed from state " + state);
    }
    OffsetDateTime now = TimeUtils.utcNow();

    if (state == InstanceState.STOPPED) {
      transitionOrConflict(
          instance, InstanceState.STOPPING, InstanceEventType.DESTROY_DISPATCHED, now);
    } else {
      instanceEventRepository.save(
          new InstanceEvent(instance, now, InstanceEventType.DESTROY_DISPATCHED, null));
    }

    dispatchNodeCommand(
        "destroy", () -> commandDispatcherService.sendDestroyInstance(node, instance));
    return InstanceDto.fromEntity(instance, templateAssignmentResolver.resolveForInstance(id));
  }

  public void reportInstancePrepared(UUID nodeId, UUID instanceId) {
    reportInstancePrepared(nodeId, instanceId, null);
  }

  public void reportInstancePrepared(UUID nodeId, UUID instanceId, String portsJson) {
    Instance instance = instanceNodeResolutionService.loadInstanceForNode(nodeId, instanceId);
    if (portsJson != null) {
      instance.updatePortsJson(portsJson);
    }
    OffsetDateTime now = TimeUtils.utcNow();
    if (instance.getState() == InstanceState.REQUESTED) {
      OffsetDateTime dispatchedAt = now.minusNanos(1_000);
      instanceStateMachine.transition(
          instance, InstanceState.PREPARING, InstanceEventType.PREPARE_DISPATCHED, dispatchedAt);
      LOGGER.warn("Instance {} had state Requested when sending prepared", instanceId);
    }
    instanceStateMachine.transition(
        instance, InstanceState.STARTING, InstanceEventType.PREPARE_COMPLETED, now);
  }

  public void reportInstanceRunning(UUID nodeId, UUID instanceId) {
    Instance instance = instanceNodeResolutionService.loadInstanceForNode(nodeId, instanceId);
    OffsetDateTime now = TimeUtils.utcNow();
    instanceStateMachine.transition(
        instance, InstanceState.RUNNING, InstanceEventType.START_COMPLETED, now);
  }

  public void reportInstanceStopped(UUID nodeId, UUID instanceId) {
    Instance instance = instanceNodeResolutionService.loadInstanceForNode(nodeId, instanceId);
    OffsetDateTime now = TimeUtils.utcNow();
    instanceStateMachine.transition(
        instance, InstanceState.STOPPED, InstanceEventType.STOP_COMPLETED, now);
  }

  public void reportInstanceDestroyed(UUID nodeId, UUID instanceId) {
    Instance instance = instanceNodeResolutionService.loadInstanceForNode(nodeId, instanceId);
    OffsetDateTime now = TimeUtils.utcNow();
    instanceStateMachine.transition(
        instance, InstanceState.DESTROYED, InstanceEventType.DESTROY_COMPLETED, now);
  }

  public void reportInstanceFailed(UUID nodeId, UUID instanceId) {
    Instance instance = instanceNodeResolutionService.loadInstanceForNode(nodeId, instanceId);
    OffsetDateTime now = TimeUtils.utcNow();
    instanceStateMachine.transition(
        instance, InstanceState.FAILED, InstanceEventType.FAILURE_REPORTED, now, null);
  }

  private Instance loadInstance(UUID instanceId) {
    return instanceRepository
        .findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
  }

  private void dispatchNodeCommand(String action, Runnable command) {
    try {
      command.run();
    } catch (ResourceAccessException ex) {
      LOGGER.warn("Node command failed to reach node. action={}", action, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Unable to reach node for " + action + " command", ex);
    } catch (HttpStatusCodeException ex) {
      LOGGER.warn(
          "Node command rejected by node. action={} status={}", action, ex.getStatusCode(), ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Node rejected " + action + " command: " + ex.getStatusCode(),
          ex);
    } catch (IllegalStateException ex) {
      LOGGER.warn(
          "Node command failed preflight. action={} message={}", action, ex.getMessage(), ex);
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
    }
  }

  private void transitionOrConflict(
      Instance instance,
      InstanceState targetState,
      InstanceEventType eventType,
      OffsetDateTime timestamp) {
    try {
      LOGGER.info(
          "Transitioning instance {} to state {} with event {} at {}",
          instance.getId(),
          targetState,
          eventType,
          timestamp);
      instanceStateMachine.transition(instance, targetState, eventType, timestamp);
    } catch (InvalidInstanceStateTransitionException ex) {
      LOGGER.warn(
          "Invalid instance transition. instanceId={} currentState={} targetState={} eventType={}",
          instance.getId(),
          instance.getState(),
          targetState,
          eventType,
          ex);
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
    }
  }
}
