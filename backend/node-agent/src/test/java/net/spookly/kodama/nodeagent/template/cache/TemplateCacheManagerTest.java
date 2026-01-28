package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateCacheManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void purgeAllRemovesAllTemplatesAndPreservesRoot() throws Exception {
        TemplateCacheLayout layout = createLayout();
        TemplateCacheManager manager = new TemplateCacheManager(layout);

        createTemplateVersion(layout, "starter", "1.0.0", "abc", "1234");
        createTemplateVersion(layout, "survival", "2.0.0", "hello", "world");

        TemplateCachePurgeResult result = manager.purgeAll();

        assertThat(result.deletedFiles()).isEqualTo(4);
        assertThat(result.deletedDirectories()).isEqualTo(6);
        assertThat(result.deletedBytes()).isEqualTo(5 + 5 + 3 + 4);
        assertThat(Files.exists(layout.getTemplatesRoot())).isTrue();
        try (var stream = Files.list(layout.getTemplatesRoot())) {
            assertThat(stream.count()).isZero();
        }
    }

    @Test
    void purgeTemplateRemovesOnlyRequestedTemplate() throws Exception {
        TemplateCacheLayout layout = createLayout();
        TemplateCacheManager manager = new TemplateCacheManager(layout);

        createTemplateVersion(layout, "starter", "1.0.0", "abc", "1234");
        createTemplateVersion(layout, "survival", "2.0.0", "hello", "world");

        TemplateCachePurgeResult result = manager.purgeTemplate("starter");

        assertThat(result.deletedFiles()).isEqualTo(2);
        assertThat(result.deletedDirectories()).isEqualTo(3);
        assertThat(result.deletedBytes()).isEqualTo(3 + 4);
        assertThat(Files.exists(layout.resolveTemplateRoot("starter"))).isFalse();
        assertThat(Files.exists(layout.resolveTemplateRoot("survival"))).isTrue();
    }

    @Test
    void purgeTemplateRejectsInvalidTemplateId() {
        TemplateCacheManager manager = new TemplateCacheManager(createLayout());

        assertThatThrownBy(() -> manager.purgeTemplate("../escape"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
    }

    private TemplateCacheLayout createLayout() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());
        return new TemplateCacheLayout(config);
    }

    private void createTemplateVersion(
            TemplateCacheLayout layout,
            String templateId,
            String version,
            String firstFileContent,
            String secondFileContent
    ) throws Exception {
        TemplateCachePaths paths = layout.resolveTemplateVersion(templateId, version);
        Files.createDirectories(paths.contentsDir());
        Files.writeString(paths.contentsDir().resolve("content.txt"), firstFileContent);
        Files.writeString(paths.versionRoot().resolve("checksum.sha256"), secondFileContent);
    }
}
