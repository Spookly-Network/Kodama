package net.spookly.kodama.nodeagent.template.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceVariableSubstitutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateLayerMergeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void mergesLayersWithLastLayerWinning() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "config/settings.yml", "from-base");
        Path layerTwo = createLayer("overlay", "1.0.0", "config/settings.yml", "from-overlay");
        Path targetDir = tempDir.resolve("workspace");

        service.mergeLayers(
                "instance-1",
                targetDir,
                List.of(
                        new TemplateLayerSource("base", "1.0.0", 0, layerOne),
                        new TemplateLayerSource("overlay", "1.0.0", 1, layerTwo)
                )
        );

        assertThat(Files.readString(targetDir.resolve("config/settings.yml"))).isEqualTo("from-overlay");
    }

    @Test
    void mergesDirectoryTreesWithoutRemovingEarlierFiles() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "maps/base.txt", "base");
        Path layerTwo = createLayer("overlay", "1.0.0", "maps/addon.txt", "addon");
        Path targetDir = tempDir.resolve("workspace");

        service.mergeLayers(
                "instance-2",
                targetDir,
                List.of(
                        new TemplateLayerSource("base", "1.0.0", 0, layerOne),
                        new TemplateLayerSource("overlay", "1.0.0", 1, layerTwo)
                )
        );

        assertThat(Files.readString(targetDir.resolve("maps/base.txt"))).isEqualTo("base");
        assertThat(Files.readString(targetDir.resolve("maps/addon.txt"))).isEqualTo("addon");
    }

    @Test
    void sortsLayersByOrderIndexBeforeMerging() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "server.txt", "from-base");
        Path layerTwo = createLayer("overlay", "1.0.0", "server.txt", "from-overlay");
        Path targetDir = tempDir.resolve("workspace");

        service.mergeLayers(
                "instance-3",
                targetDir,
                List.of(
                        new TemplateLayerSource("overlay", "1.0.0", 2, layerTwo),
                        new TemplateLayerSource("base", "1.0.0", 1, layerOne)
                )
        );

        assertThat(Files.readString(targetDir.resolve("server.txt"))).isEqualTo("from-overlay");
    }

    @Test
    void rejectsDuplicateOrderIndexes() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "a.txt", "one");
        Path layerTwo = createLayer("overlay", "1.0.0", "b.txt", "two");

        assertThatThrownBy(() -> service.mergeLayers(
                "instance-4",
                tempDir.resolve("workspace"),
                List.of(
                        new TemplateLayerSource("base", "1.0.0", 0, layerOne),
                        new TemplateLayerSource("overlay", "1.0.0", 0, layerTwo)
                )
        ))
                .isInstanceOf(TemplateLayerMergeException.class)
                .hasMessageContaining("Duplicate template layer orderIndex");
    }

    @Test
    void replacesDirectoryWithFileWhenLaterLayerOverrides() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "conflict/file.txt", "dir-content");
        Path layerTwo = createLayer("overlay", "1.0.0", "conflict", "file-content");
        Path targetDir = tempDir.resolve("workspace");

        service.mergeLayers(
                "instance-5",
                targetDir,
                List.of(
                        new TemplateLayerSource("base", "1.0.0", 0, layerOne),
                        new TemplateLayerSource("overlay", "1.0.0", 1, layerTwo)
                )
        );

        assertThat(Files.isRegularFile(targetDir.resolve("conflict"))).isTrue();
        assertThat(Files.readString(targetDir.resolve("conflict"))).isEqualTo("file-content");
        assertThat(Files.exists(targetDir.resolve("conflict/file.txt"))).isFalse();
    }

    @Test
    void clearsExistingWorkspaceBeforeMerging() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "config/current.yml", "current");
        Path targetDir = tempDir.resolve("workspace");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("stale.txt"), "stale");

        service.mergeLayers(
                "instance-6",
                targetDir,
                List.of(new TemplateLayerSource("base", "1.0.0", 0, layerOne))
        );

        assertThat(Files.exists(targetDir.resolve("stale.txt"))).isFalse();
        assertThat(Files.readString(targetDir.resolve("config/current.yml"))).isEqualTo("current");
    }

    @Test
    void substitutesVariablesAfterMerge() throws Exception {
        TemplateLayerMergeService service = createService();
        Path layerOne = createLayer("base", "1.0.0", "config/server.properties", "name=${SERVER_NAME}");
        Path targetDir = tempDir.resolve("workspace");

        service.mergeLayers(
                "instance-7",
                targetDir,
                List.of(new TemplateLayerSource("base", "1.0.0", 0, layerOne)),
                Map.of("SERVER_NAME", "alpha")
        );

        assertThat(Files.readString(targetDir.resolve("config/server.properties"))).isEqualTo("name=alpha");
    }

    private TemplateLayerMergeService createService() {
        NodeConfig config = new NodeConfig();
        return new TemplateLayerMergeService(new InstanceVariableSubstitutionService(config));
    }

    private Path createLayer(String templateId, String version, String relativeFile, String contents) throws Exception {
        Path root = tempDir.resolve(templateId + "-" + version);
        Path filePath = root.resolve(relativeFile);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, contents);
        return root;
    }
}
