package net.spookly.kodama.nodeagent.docker.dto;

public record DockerContainerSummary(
        String containerId,
        String image,
        String[] names,
        String state,
        String status
) {
}
