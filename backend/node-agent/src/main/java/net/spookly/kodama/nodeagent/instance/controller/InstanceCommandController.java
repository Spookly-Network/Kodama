package net.spookly.kodama.nodeagent.instance.controller;

import java.util.UUID;

import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareService;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/instances")
public class InstanceCommandController {

    private final InstancePrepareService prepareService;

    public InstanceCommandController(InstancePrepareService prepareService) {
        this.prepareService = prepareService;
    }

    @PostMapping("/{instanceId}/prepare")
    @ResponseStatus(HttpStatus.OK)
    public void prepare(
            @PathVariable UUID instanceId,
            @RequestBody(required = false) NodePrepareInstanceRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (request.instanceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId is required");
        }
        if (!instanceId.equals(request.instanceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId does not match path");
        }
        try {
            prepareService.prepare(request);
        } catch (InstancePrepareValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
