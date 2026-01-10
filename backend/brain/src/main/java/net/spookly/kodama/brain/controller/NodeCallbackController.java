package net.spookly.kodama.brain.controller;

import java.util.UUID;

import net.spookly.kodama.brain.service.InstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes/{nodeId}/instances/{instanceId}")
public class NodeCallbackController {

    private final InstanceService instanceService;

    public NodeCallbackController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @PostMapping("/prepared")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void prepared(@PathVariable UUID nodeId, @PathVariable UUID instanceId) {
        instanceService.reportInstancePrepared(nodeId, instanceId);
    }

    @PostMapping("/running")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void running(@PathVariable UUID nodeId, @PathVariable UUID instanceId) {
        instanceService.reportInstanceRunning(nodeId, instanceId);
    }

    @PostMapping("/stopped")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void stopped(@PathVariable UUID nodeId, @PathVariable UUID instanceId) {
        instanceService.reportInstanceStopped(nodeId, instanceId);
    }

    @PostMapping("/destroyed")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void destroyed(@PathVariable UUID nodeId, @PathVariable UUID instanceId) {
        instanceService.reportInstanceDestroyed(nodeId, instanceId);
    }

    @PostMapping("/failed")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERATOR')")
    public void failed(@PathVariable UUID nodeId, @PathVariable UUID instanceId) {
        instanceService.reportInstanceFailed(nodeId, instanceId);
    }
}
