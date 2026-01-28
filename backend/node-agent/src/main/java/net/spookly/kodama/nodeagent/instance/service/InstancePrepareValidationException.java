package net.spookly.kodama.nodeagent.instance.service;

public class InstancePrepareValidationException extends InstancePrepareException {

    public InstancePrepareValidationException(String message) {
        super(message);
    }

    public InstancePrepareValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
