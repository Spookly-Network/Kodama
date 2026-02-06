package net.spookly.kodama.nodeagent.health.controller;

import java.util.UUID;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.health.dto.NodeHealthResponse;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final NodeConfig config;
    private final NodeRegistrationState registrationState;

    public HealthController(NodeConfig config, NodeRegistrationState registrationState) {
        this.config = config;
        this.registrationState = registrationState;
    }

    @GetMapping("/health")
    public NodeHealthResponse health() {
        return new NodeHealthResponse(
                "ok",
                resolveNodeId(),
                config.getNodeName(),
                config.getNodeVersion()
        );
    }

    private String resolveNodeId() {
        UUID registered = registrationState.getNodeId();
        if (registered != null) {
            return registered.toString();
        }
        String configured = config.getNodeId();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return configured.trim();
    }
}
