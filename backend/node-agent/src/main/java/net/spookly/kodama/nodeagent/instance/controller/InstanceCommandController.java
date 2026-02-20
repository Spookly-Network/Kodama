package net.spookly.kodama.nodeagent.instance.controller;

import java.util.List;
import java.util.UUID;
import net.spookly.kodama.nodeagent.instance.dto.NodeInstanceCommandRequest;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.service.InstanceLifecycleService;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareService;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
  private final InstanceLifecycleService lifecycleService;
  private final InstanceRegistryService registryService;

  public InstanceCommandController(
      InstancePrepareService prepareService,
      InstanceLifecycleService lifecycleService,
      InstanceRegistryService registryService) {
    this.prepareService = prepareService;
    this.lifecycleService = lifecycleService;
    this.registryService = registryService;
  }

  @GetMapping("/registry")
  public List<InstanceRegistryEntry> listRegistries() {
    return registryService.listRegistries();
  }

  @PostMapping("/{instanceId}/prepare")
  @ResponseStatus(HttpStatus.OK)
  public void prepare(
      @PathVariable UUID instanceId,
      @RequestBody(required = false) NodePrepareInstanceRequest request) {
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

  @PostMapping("/{instanceId}/start")
  @ResponseStatus(HttpStatus.OK)
  public void start(
      @PathVariable UUID instanceId,
      @RequestBody(required = false) NodeInstanceCommandRequest request) {
    NodeInstanceCommandRequest validated = requireCommand(instanceId, request);
    try {
      lifecycleService.start(validated);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }

  @PostMapping("/{instanceId}/stop")
  @ResponseStatus(HttpStatus.OK)
  public void stop(
      @PathVariable UUID instanceId,
      @RequestBody(required = false) NodeInstanceCommandRequest request) {
    NodeInstanceCommandRequest validated = requireCommand(instanceId, request);
    try {
      lifecycleService.stop(validated);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }

  @PostMapping("/{instanceId}/destroy")
  @ResponseStatus(HttpStatus.OK)
  public void destroy(
      @PathVariable UUID instanceId,
      @RequestBody(required = false) NodeInstanceCommandRequest request) {
    NodeInstanceCommandRequest validated = requireCommand(instanceId, request);
    try {
      lifecycleService.destroy(validated);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }

  private NodeInstanceCommandRequest requireCommand(
      UUID instanceId, NodeInstanceCommandRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
    }
    if (request.instanceId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId is required");
    }
    if (!instanceId.equals(request.instanceId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId does not match path");
    }
    return request;
  }
}
