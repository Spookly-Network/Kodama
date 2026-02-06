package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeHeartbeatRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.dto.NodeRegistrationResponse;
import net.spookly.kodama.brain.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR','ROLE_VIEWER')")
    public List<NodeDto> listNodes() {
        return nodeService.listNodes();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ROLE_NODE')")
    public NodeRegistrationResponse registerNode(@Valid @RequestBody NodeRegistrationRequest request) {
        return nodeService.registerNode(request);
    }

    @PostMapping("/{nodeId}/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ROLE_NODE')")
    public NodeDto heartbeat(
            @PathVariable UUID nodeId,
            @Valid @RequestBody NodeHeartbeatRequest request) {
        return nodeService.heartbeat(nodeId, request);
    }
}
