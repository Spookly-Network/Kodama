package net.spookly.kodama.nodeagent.instance.service;

public class InstancePrepareException extends RuntimeException {

    public InstancePrepareException(String message) {
        super(message);
    }

    public InstancePrepareException(String message, Throwable cause) {
        super(message, cause);
    }
}
