package net.spookly.kodama.brain.service;

import java.time.OffsetDateTime;
import java.util.List;

import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Node node = nodeRepository.findByName(request.getName())
                .map(existing -> refreshHeartbeat(existing, request))
                .orElseGet(() -> createNode(request));

        return NodeDto.fromEntity(node);
    }

    private Node refreshHeartbeat(Node existing, NodeRegistrationRequest request) {
        existing.updateHeartbeat(request.getStatus(), OffsetDateTime.now(), request.getCapacity());
        return existing;
    }

    private Node createNode(NodeRegistrationRequest request) {
        Node node = new Node(
                request.getName(),
                request.getRegion(),
                request.getStatus() == null ? NodeStatus.OFFLINE : request.getStatus(),
                OffsetDateTime.now(),
                request.getCapacity()
        );
        return nodeRepository.save(node);
    }
}
