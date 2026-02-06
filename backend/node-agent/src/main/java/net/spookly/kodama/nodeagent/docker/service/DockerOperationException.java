package net.spookly.kodama.nodeagent.docker.service;

public class DockerOperationException extends RuntimeException {

    public DockerOperationException(String message) {
        super(message);
    }

    public DockerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
