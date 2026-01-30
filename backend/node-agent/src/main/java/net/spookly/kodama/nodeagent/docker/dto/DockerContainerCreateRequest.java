package net.spookly.kodama.nodeagent.docker.dto;

import java.util.List;
import java.util.Map;

public record DockerContainerCreateRequest(
        String image,
        String name,
        List<String> command,
        List<String> env,
        Map<String, String> labels,
        String workingDir
) {
}
