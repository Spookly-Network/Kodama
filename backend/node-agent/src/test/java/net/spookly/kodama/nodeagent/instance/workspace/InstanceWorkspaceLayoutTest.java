package net.spookly.kodama.nodeagent.instance.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceWorkspaceLayoutTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesWorkspacePaths() {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace-root").toString());

        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);
        InstanceWorkspacePaths paths = layout.resolveWorkspace("instance-1");

        Path expectedInstanceRoot = layout.getInstancesRoot().resolve("instance-1");

        assertThat(paths.instanceId()).isEqualTo("instance-1");
        assertThat(paths.instanceRoot()).isEqualTo(expectedInstanceRoot);
        assertThat(paths.mergedDir()).isEqualTo(expectedInstanceRoot.resolve("merged"));
        assertThat(paths.logsDir()).isEqualTo(expectedInstanceRoot.resolve("logs"));
        assertThat(paths.tempDir()).isEqualTo(expectedInstanceRoot.resolve("temp"));
    }

    @Test
    void rejectsInvalidInstanceId() {
        NodeConfig config = new NodeConfig();
        config.setWorkspaceDir(tempDir.resolve("workspace-root").toString());

        InstanceWorkspaceLayout layout = new InstanceWorkspaceLayout(config);

        assertThatThrownBy(() -> layout.resolveWorkspace("../escape"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instanceId");
    }
}
