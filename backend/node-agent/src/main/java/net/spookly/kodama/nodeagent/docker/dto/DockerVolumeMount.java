package net.spookly.kodama.nodeagent.docker.dto;

public record DockerVolumeMount(
        String hostPath,
        String containerPath,
        boolean readOnly
) {
}
