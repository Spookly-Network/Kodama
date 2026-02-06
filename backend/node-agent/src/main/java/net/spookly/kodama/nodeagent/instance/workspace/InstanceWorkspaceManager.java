package net.spookly.kodama.nodeagent.instance.workspace;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceWorkspaceManager {

    private static final Logger logger = LoggerFactory.getLogger(InstanceWorkspaceManager.class);

    private final InstanceWorkspaceLayout layout;

    public InstanceWorkspaceManager(InstanceWorkspaceLayout layout) {
        this.layout = layout;
    }

    public InstanceWorkspacePaths prepareWorkspace(String instanceId) {
        InstanceWorkspacePaths paths = layout.resolveWorkspace(instanceId);
        try {
            Files.createDirectories(paths.instanceRoot());
            Files.createDirectories(paths.mergedDir());
            Files.createDirectories(paths.logsDir());
            Files.createDirectories(paths.tempDir());
        } catch (IOException ex) {
            throw new InstanceWorkspaceException(
                    "Failed to create workspace for instance " + instanceId + " at " + paths.instanceRoot(),
                    ex
            );
        }
        logger.debug("Workspace ready for instance {} at {}", paths.instanceId(), paths.instanceRoot());
        return paths;
    }

    public void deleteWorkspace(InstanceWorkspacePaths workspace) {
        if (workspace == null) {
            throw new InstanceWorkspaceException("instance workspace is required");
        }
        Path instanceRoot = workspace.instanceRoot();
        if (instanceRoot == null) {
            throw new InstanceWorkspaceException("instance workspace root is required");
        }
        if (!Files.exists(instanceRoot)) {
            return;
        }
        ensureWithinInstancesRoot(layout.getInstancesRoot(), instanceRoot);
        try {
            Files.walkFileTree(instanceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new InstanceWorkspaceException(
                    "Failed to delete workspace for instance " + workspace.instanceId() + " at " + instanceRoot,
                    ex
            );
        }
        logger.info("Workspace deleted for instance {} at {}", workspace.instanceId(), instanceRoot);
    }

    private void ensureWithinInstancesRoot(Path instancesRoot, Path candidate) {
        if (instancesRoot == null || candidate == null) {
            throw new InstanceWorkspaceException("Instance workspace root path cannot be null");
        }
        Path normalizedRoot = instancesRoot.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new InstanceWorkspaceException(
                    "Refusing to delete path outside instances root. root=" + normalizedRoot + " path=" + normalizedCandidate
            );
        }
    }
}
