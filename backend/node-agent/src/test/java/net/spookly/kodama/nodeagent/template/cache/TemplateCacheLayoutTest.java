package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateCacheLayoutTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesTemplateVersionPaths() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());

        TemplateCacheLayout layout = new TemplateCacheLayout(config);
        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");

        Path expectedVersionRoot = layout.getTemplatesRoot().resolve("starter").resolve("1.2.3");

        assertThat(paths.templateId()).isEqualTo("starter");
        assertThat(paths.version()).isEqualTo("1.2.3");
        assertThat(paths.templateRoot()).isEqualTo(layout.getTemplatesRoot().resolve("starter"));
        assertThat(paths.versionRoot()).isEqualTo(expectedVersionRoot);
        assertThat(paths.contentsDir()).isEqualTo(expectedVersionRoot.resolve("contents"));
        assertThat(paths.checksumFile()).isEqualTo(expectedVersionRoot.resolve("checksum.sha256"));
        assertThat(paths.metadataFile()).isEqualTo(expectedVersionRoot.resolve("metadata.json"));
    }

    @Test
    void rejectsInvalidSegments() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());

        TemplateCacheLayout layout = new TemplateCacheLayout(config);

        assertThatThrownBy(() -> layout.resolveTemplateVersion("../escape", "1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
        assertThatThrownBy(() -> layout.resolveTemplateVersion("starter", "../1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }
}
