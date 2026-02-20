package net.spookly.kodama.brain.plugin;

import java.util.UUID;

public record BrainPrepareInstanceLayer(
    UUID templateVersionId,
    UUID templateId,
    String version,
    String checksum,
    String s3Key,
    String metadataJson,
    int orderIndex) {}
