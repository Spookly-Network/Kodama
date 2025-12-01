package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeHeartbeatRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NodeService {

    private final NodeRepository nodeRepository;

    public NodeService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public List<NodeDto> listNodes() {
        return nodeRepository.findAll()
                .stream()
                .map(NodeDto::fromEntity)
                .toList();
    }

    public NodeDto registerNode(NodeRegistrationRequest request) {
        validateRegistration(request);
        Node node = nodeRepository.findByName(request.getName())
                .map(existing -> refreshRegistration(existing, request))
                .orElseGet(() -> createNode(request));

        return NodeDto.fromEntity(node);
    }

    public NodeDto heartbeat(UUID nodeId, NodeHeartbeatRequest request) {
        Node node = nodeRepository.findById(nodeId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        validateHeartbeat(node, request);
        node.updateHeartbeat(request.getStatus(), request.getUsedSlots(), OffsetDateTime.now(ZoneOffset.UTC));
        return NodeDto.fromEntity(node);
    }

    private Node refreshRegistration(Node existing, NodeRegistrationRequest request) {
        existing.updateRegistration(
                request.getRegion(),
                request.isDevMode(),
                request.getCapacitySlots(),
                request.getNodeVersion(),
                request.getTags(),
                request.getStatus()
        );
        return existing;
    }

    private Node createNode(NodeRegistrationRequest request) {
        Node node = new Node(
                request.getName(),
                request.getRegion(),
                Objects.requireNonNullElse(request.getStatus(), NodeStatus.UNKNOWN),
                request.isDevMode(),
                request.getCapacitySlots(),
                0,
                OffsetDateTime.now(ZoneOffset.UTC),
                request.getNodeVersion(),
                request.getTags()
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
