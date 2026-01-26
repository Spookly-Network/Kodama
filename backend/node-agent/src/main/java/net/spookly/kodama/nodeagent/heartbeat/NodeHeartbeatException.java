package net.spookly.kodama.nodeagent.heartbeat;

public class NodeHeartbeatException extends RuntimeException {

    public NodeHeartbeatException(String message) {
        super(message);
    }

    public NodeHeartbeatException(String message, Throwable cause) {
        super(message, cause);
    }
}
