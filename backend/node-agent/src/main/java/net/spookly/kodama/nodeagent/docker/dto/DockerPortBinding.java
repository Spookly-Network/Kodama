package net.spookly.kodama.nodeagent.docker.dto;

public record DockerPortBinding(
        int containerPort,
        int hostPort,
        String protocol
) {
}
