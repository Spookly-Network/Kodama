package net.spookly.kodama.nodeagent.instance.workspace;

import java.nio.file.Path;

public record InstanceWorkspacePaths(
        String instanceId,
        Path instanceRoot,
        Path mergedDir,
        Path logsDir,
        Path tempDir
) {
}
