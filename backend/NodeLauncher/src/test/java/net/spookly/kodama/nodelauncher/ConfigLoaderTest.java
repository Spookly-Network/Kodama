package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

  @TempDir Path tempDir;

  @Test
  void shouldCreateDefaultConfigWhenMissing() throws IOException {
    Path configPath = tempDir.resolve("config.yml");
    ConfigLoader loader = new ConfigLoader(key -> null, configPath);

    LauncherConfig config = loader.load();

    assertTrue(Files.isRegularFile(configPath));
    //TODO: Get from elsewhere
    assertEquals("Spookly-Network", config.github().owner());
    assertEquals("Kodama", config.github().repo());
    assertEquals(LauncherConfig.Channel.STABLE, config.github().channel());
    assertEquals(LauncherConfig.UpdateMode.NEXT_START, config.updateMode());
    assertEquals("/opt/kodama-node", config.installDir());
  }

  @Test
  void shouldUseEnvironmentConfigPathWhenProvided() {
    Path envConfigPath = tempDir.resolve("custom").resolve("config.yml");
    ConfigLoader loader =
        new ConfigLoader(
            key -> ConfigLoader.CONFIG_PATH_ENV.equals(key) ? envConfigPath.toString() : null,
            tempDir.resolve("ignored.yml"));

    LauncherConfig config = loader.load();

    assertTrue(Files.isRegularFile(envConfigPath));
    assertEquals("Spookly-Network", config.github().owner());
  }

  @Test
  void shouldFailWhenResolvedPathIsNotAFile() throws IOException {
    Path configDirectory = tempDir.resolve("config.yml");
    Files.createDirectory(configDirectory);
    ConfigLoader loader = new ConfigLoader(key -> null, configDirectory);

    assertThrows(IllegalStateException.class, loader::load);
  }
}
