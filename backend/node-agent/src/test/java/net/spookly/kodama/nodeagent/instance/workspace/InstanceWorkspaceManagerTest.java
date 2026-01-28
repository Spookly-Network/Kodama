package net.spookly.kodama.nodeagent.instance.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceWorkspaceManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void prepareWorkspaceCreatesDirectories() {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace-root").toString());

        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);
        InstanceWorkspaceManager manager = new InstanceWorkspaceManager(layout);

        InstanceWorkspacePaths paths = manager.prepareWorkspace("instance-42");

        assertThat(Files.isDirectory(paths.instanceRoot())).isTrue();
        assertThat(Files.isDirectory(paths.mergedDir())).isTrue();
        assertThat(Files.isDirectory(paths.logsDir())).isTrue();
        assertThat(Files.isDirectory(paths.tempDir())).isTrue();
    }
}
