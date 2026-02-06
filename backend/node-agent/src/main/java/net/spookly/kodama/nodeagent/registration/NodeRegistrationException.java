package net.spookly.kodama.nodeagent.registration;

public class NodeRegistrationException extends RuntimeException {

    public NodeRegistrationException(String message) {
        super(message);
    }

    public NodeRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
