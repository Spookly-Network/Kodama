package net.spookly.kodama.nodeagent.instance.service;

public class InstanceStartException extends RuntimeException {

    public InstanceStartException(String message) {
        super(message);
    }

    public InstanceStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
