package net.spookly.kodama.nodeagent.docker.dto;

import java.util.List;

public record DockerContainerCreateResult(
        String containerId,
        List<String> warnings
) {
}
