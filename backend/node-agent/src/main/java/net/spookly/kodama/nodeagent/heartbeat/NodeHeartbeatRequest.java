package net.spookly.kodama.nodeagent.heartbeat;

public class NodeHeartbeatRequest {

    private NodeStatus status;
    private int usedSlots;

    public NodeHeartbeatRequest() {
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public int getUsedSlots() {
        return usedSlots;
    }

    public void setUsedSlots(int usedSlots) {
        this.usedSlots = usedSlots;
    }
}
