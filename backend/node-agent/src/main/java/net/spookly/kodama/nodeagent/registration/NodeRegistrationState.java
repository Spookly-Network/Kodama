package net.spookly.kodama.nodeagent.registration;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class NodeRegistrationState {

    private final AtomicReference<UUID> nodeId = new AtomicReference<>();
    private final AtomicInteger heartbeatIntervalSeconds = new AtomicInteger();

    public void update(NodeRegistrationResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response is required");
        }
        nodeId.set(response.getNodeId());
        heartbeatIntervalSeconds.set(response.getHeartbeatIntervalSeconds());
    }

    public UUID getNodeId() {
        return nodeId.get();
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds.get();
    }
}
