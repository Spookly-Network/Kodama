package net.spookly.kodama.nodeagent.heartbeat;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class NodeHeartbeatState {

    private final AtomicInteger usedSlots = new AtomicInteger();
    private final AtomicReference<NodeStatus> status = new AtomicReference<>(NodeStatus.ONLINE);

    public int getUsedSlots() {
        return usedSlots.get();
    }

    public void setUsedSlots(int usedSlots) {
        if (usedSlots < 0) {
            throw new IllegalArgumentException("usedSlots cannot be negative");
        }
        this.usedSlots.set(usedSlots);
    }

    public NodeStatus getStatus() {
        return status.get();
    }

    public void setStatus(NodeStatus status) {
        this.status.set(Objects.requireNonNull(status, "status"));
    }
}
