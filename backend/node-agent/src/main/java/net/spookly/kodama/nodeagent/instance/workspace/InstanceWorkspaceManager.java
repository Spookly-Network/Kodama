package net.spookly.kodama.nodeagent.instance.workspace;

import java.io.IOException;
import java.nio.file.Files;

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
}
