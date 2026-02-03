package net.spookly.kodama.nodeagent.instance.dto;

import java.util.UUID;

public record NodePrepareInstanceLayer(
        UUID templateVersionId,
        UUID templateId,
        String version,
        String checksum,
        String s3Key,
        String metadataJson,
        int orderIndex
) {
}
