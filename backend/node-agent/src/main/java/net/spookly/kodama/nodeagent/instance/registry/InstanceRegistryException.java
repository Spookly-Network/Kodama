package net.spookly.kodama.nodeagent.instance.registry;

public class InstanceRegistryException extends RuntimeException {

    public InstanceRegistryException(String message) {
        super(message);
    }

    public InstanceRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
