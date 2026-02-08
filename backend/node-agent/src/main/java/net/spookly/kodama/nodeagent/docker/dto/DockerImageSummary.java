package net.spookly.kodama.nodeagent.docker.dto;

public record DockerImageSummary(
        String imageId,
        String[] repoTags,
        Long size,
        Long created
) {
}
