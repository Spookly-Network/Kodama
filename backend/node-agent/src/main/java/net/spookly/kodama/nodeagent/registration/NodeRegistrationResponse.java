package net.spookly.kodama.nodeagent.registration;

import java.util.UUID;

public class NodeRegistrationResponse {

    private UUID nodeId;
    private int heartbeatIntervalSeconds;

    public NodeRegistrationResponse() {
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
}
