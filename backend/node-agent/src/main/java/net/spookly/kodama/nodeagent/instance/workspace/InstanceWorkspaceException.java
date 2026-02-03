package net.spookly.kodama.nodeagent.instance.workspace;

public class InstanceWorkspaceException extends RuntimeException {

    public InstanceWorkspaceException(String message) {
        super(message);
    }

    public InstanceWorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
