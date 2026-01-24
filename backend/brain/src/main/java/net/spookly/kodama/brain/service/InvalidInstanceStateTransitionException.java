package net.spookly.kodama.brain.service;

public class InvalidInstanceStateTransitionException extends RuntimeException {

    public InvalidInstanceStateTransitionException(String message) {
        super(message);
    }
}
