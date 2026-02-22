package net.spookly.kodama.brain.service;

import java.util.UUID;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class InstanceNodeResolutionService {

  private final NodeRepository nodeRepository;
  private final InstanceRepository instanceRepository;
  private final SchedulingService schedulingService;

  public InstanceNodeResolutionService(
      NodeRepository nodeRepository,
      InstanceRepository instanceRepository,
      SchedulingService schedulingService) {
    this.nodeRepository = nodeRepository;
    this.instanceRepository = instanceRepository;
    this.schedulingService = schedulingService;
  }

  public Node resolveNodeForCreate(CreateInstanceRequest request, int slotsRequired) {
    UUID requestedNodeId = request.getNodeId();
    if (requestedNodeId != null) {
      Node node = loadNodeOrNotFound(requestedNodeId);
      validateNodeCapacity(node, slotsRequired);
      return node;
    }

    Node scheduledNode =
        schedulingService.selectNode(
            request.getRegion(), request.getTags(), request.getDevModeAllowed(), slotsRequired);
    if (scheduledNode != null) {
      return scheduledNode;
    }

    if (schedulingService.hasEligibleNodes(
        request.getRegion(), request.getTags(), request.getDevModeAllowed())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Insufficient node capacity for slotsRequired=" + slotsRequired);
    }
    throw new ResponseStatusException(HttpStatus.CONFLICT, "No eligible nodes found");
  }

  public Instance loadInstanceForNode(UUID nodeId, UUID instanceId) {
    loadNodeOrNotFound(nodeId);
    Instance instance = loadInstanceOrNotFound(instanceId);
    if (instance.getNode() == null || instance.getNode().getId() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance is not assigned to a node");
    }
    if (!instance.getNode().getId().equals(nodeId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Instance is not assigned to the requested node");
    }
    return instance;
  }

  public Node requireAssignedNode(Instance instance) {
    Node node = instance.getNode();
    if (node == null || node.getId() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Instance is not assigned to a node");
    }
    UUID nodeId = node.getId();
    return nodeRepository
        .findById(nodeId)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Assigned node no longer exists"));
  }

  private Node loadNodeOrNotFound(UUID nodeId) {
    return nodeRepository
        .findById(nodeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
  }

  private Instance loadInstanceOrNotFound(UUID instanceId) {
    return instanceRepository
        .findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
  }

  private void validateNodeCapacity(Node node, int slotsRequired) {
    if (((long) node.getUsedSlots() + slotsRequired) > node.getCapacitySlots()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Insufficient node capacity for slotsRequired="
              + slotsRequired
              + " (usedSlots="
              + node.getUsedSlots()
              + ", capacitySlots="
              + node.getCapacitySlots()
              + ")");
    }
  }
}
