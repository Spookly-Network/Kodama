package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class TemplateCacheInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void createsCacheDirectoriesOnStartup() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());

        TemplateCacheLayout layout = new TemplateCacheLayout(config);
        TemplateCacheInitializer initializer = new TemplateCacheInitializer(layout);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(Files.isDirectory(layout.getTemplatesRoot())).isTrue();
    }
}
