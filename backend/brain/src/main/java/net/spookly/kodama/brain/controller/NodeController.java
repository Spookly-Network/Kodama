package net.spookly.kodama.brain.controller;

import java.util.List;

import jakarta.validation.Valid;
import net.spookly.kodama.brain.dto.NodeDto;
import net.spookly.kodama.brain.dto.NodeRegistrationRequest;
import net.spookly.kodama.brain.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<NodeDto> listNodes() {
        return nodeService.listNodes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NodeDto registerNode(@Valid @RequestBody NodeRegistrationRequest request) {
        return nodeService.registerNode(request);
    }
}
