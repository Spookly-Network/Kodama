package net.spookly.kodama.nodeagent.template.cache;

import java.nio.file.Path;

public record TemplateCachePaths(
        String templateId,
        String version,
        Path templateRoot,
        Path versionRoot,
        Path contentsDir,
        Path checksumFile,
        Path metadataFile
) {
}
