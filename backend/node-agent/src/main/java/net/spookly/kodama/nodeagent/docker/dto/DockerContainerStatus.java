package net.spookly.kodama.nodeagent.docker.dto;

public record DockerContainerStatus(
        String containerId,
        String name,
        String image,
        String status,
        Boolean running,
        Boolean paused,
        Boolean restarting,
        Boolean oomKilled,
        Boolean dead,
        Integer exitCode,
        String error,
        String startedAt,
        String finishedAt
) {
}
