package net.spookly.kodama.nodeagent.instance.workspace;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.stereotype.Component;

@Component
public class InstanceWorkspaceLayout {

    public static final String INSTANCES_DIR_NAME = "instances";
    public static final String MERGED_DIR_NAME = "merged";
    public static final String LOGS_DIR_NAME = "logs";
    public static final String TEMP_DIR_NAME = "temp";

    private final Path workspaceRoot;
    private final Path instancesRoot;

    public InstanceWorkspaceLayout(NodeConfig config) {
        if (config == null || config.getWorkspaceDir() == null || config.getWorkspaceDir().isBlank()) {
            throw new InstanceWorkspaceException("node-agent.workspace-dir is required to build instance workspace layout");
        }
        String workspaceDir = config.getWorkspaceDir().trim();
        try {
            this.workspaceRoot = Paths.get(workspaceDir).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new InstanceWorkspaceException("Invalid node-agent.workspace-dir: " + workspaceDir, ex);
        }
        this.instancesRoot = workspaceRoot.resolve(INSTANCES_DIR_NAME);
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path getInstancesRoot() {
        return instancesRoot;
    }

    public Path resolveInstanceRoot(String instanceId) {
        String normalizedInstanceId = requireSegment("instanceId", instanceId);
        return instancesRoot.resolve(normalizedInstanceId);
    }

    public InstanceWorkspacePaths resolveWorkspace(String instanceId) {
        String normalizedInstanceId = requireSegment("instanceId", instanceId);
        Path instanceRoot = instancesRoot.resolve(normalizedInstanceId);
        Path mergedDir = instanceRoot.resolve(MERGED_DIR_NAME);
        Path logsDir = instanceRoot.resolve(LOGS_DIR_NAME);
        Path tempDir = instanceRoot.resolve(TEMP_DIR_NAME);
        return new InstanceWorkspacePaths(
                normalizedInstanceId,
                instanceRoot,
                mergedDir,
                logsDir,
                tempDir
        );
    }

    private String requireSegment(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        String trimmed = value.trim();
        Path segment;
        try {
            segment = Paths.get(trimmed);
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException(label + " contains invalid path characters", ex);
        }
        if (segment.isAbsolute() || segment.getNameCount() != 1) {
            throw new IllegalArgumentException(label + " must be a single path segment");
        }
        String name = segment.getFileName().toString();
        if (".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException(label + " must not be '.' or '..'");
        }
        return name;
    }
}
