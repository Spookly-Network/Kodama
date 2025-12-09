package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.dto.NodeRegistrationResponse;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeHeartbeatRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.repository.NodeRepository;
import net.spookly.kodama.brain.config.NodeProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NodeService {

    private final NodeRepository nodeRepository;
    private final NodeProperties nodeProperties;

    public NodeService(NodeRepository nodeRepository, NodeProperties nodeProperties) {
        this.nodeRepository = nodeRepository;
        this.nodeProperties = nodeProperties;
    }

    public List<NodeDto> listNodes() {
        return nodeRepository.findAll()
                .stream()
                .map(NodeDto::fromEntity)
                .toList();
    }

    public NodeRegistrationResponse registerNode(NodeRegistrationRequest request) {
        validateRegistration(request);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Node node = nodeRepository.findByName(request.getName())
                .map(existing -> refreshRegistration(existing, request, now))
                .orElseGet(() -> createNode(request, now));

        return new NodeRegistrationResponse(
                node.getId(),
                nodeProperties.getHeartbeatIntervalSeconds()
        );
    }

    public NodeDto heartbeat(UUID nodeId, NodeHeartbeatRequest request) {
        Node node = nodeRepository.findById(nodeId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        validateHeartbeat(node, request);
        node.updateHeartbeat(request.getStatus(), request.getUsedSlots(), OffsetDateTime.now(ZoneOffset.UTC));
        return NodeDto.fromEntity(node);
    }

    private Node refreshRegistration(Node existing, NodeRegistrationRequest request, OffsetDateTime now) {
        NodeStatus status = request.getStatus() != null ? request.getStatus() : NodeStatus.UNKNOWN;
        int usedSlots = Math.min(existing.getUsedSlots(), request.getCapacitySlots());
        existing.updateRegistration(
                request.getRegion(),
                request.isDevMode(),
                request.getCapacitySlots(),
                request.getNodeVersion(),
                request.getTags(),
                status,
                request.getBaseUrl()
        );
        existing.updateHeartbeat(status, usedSlots, now);
        return existing;
    }

    private Node createNode(NodeRegistrationRequest request, OffsetDateTime now) {
        NodeStatus status = request.getStatus() != null ? request.getStatus() : NodeStatus.UNKNOWN;
        Node node = new Node(
                request.getName(),
                request.getRegion(),
                status,
                request.isDevMode(),
                request.getCapacitySlots(),
                0,
                now,
                request.getNodeVersion(),
                request.getTags(),
                request.getBaseUrl()
        );
        return nodeRepository.save(node);
    }

    private void validateRegistration(NodeRegistrationRequest request) {
        if (request.getCapacitySlots() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "capacitySlots must be at least 1");
        }
    }

    private void validateHeartbeat(Node node, NodeHeartbeatRequest request) {
        if (request.getUsedSlots() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "usedSlots cannot be negative");
        }
        if (request.getUsedSlots() > node.getCapacitySlots()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "usedSlots cannot be greater than capacitySlots");
        }
    }
}
