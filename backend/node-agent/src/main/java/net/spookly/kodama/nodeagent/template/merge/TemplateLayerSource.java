package net.spookly.kodama.nodeagent.template.merge;

import java.nio.file.Path;

public record TemplateLayerSource(
        String templateId,
        String version,
        int orderIndex,
        Path contentsDir
) {
}
