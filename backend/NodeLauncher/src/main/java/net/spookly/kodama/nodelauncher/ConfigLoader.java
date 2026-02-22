package net.spookly.kodama.nodelauncher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

  static final String CONFIG_PATH_ENV = "KODAMA_LAUNCHER_CONFIG";
  private static final String DEFAULT_CONFIG_PATH = "./config.yml";

  private final ObjectMapper objectMapper;

  public ConfigLoader() {
    this.objectMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
  }

  public LauncherConfig load() {
    Path configPath = resolveConfigPath();
    if (!Files.isRegularFile(configPath)) {
      throw new IllegalStateException("Launcher config file does not exist: " + configPath);
    }
    try {
      LauncherConfig config = objectMapper.readValue(configPath.toFile(), LauncherConfig.class);
      if (config.updateMode() != LauncherConfig.UpdateMode.NEXT_START) {
        throw new IllegalStateException(
            "Unsupported update mode: " + config.updateMode() + ". Only NEXT_START is supported.");
      }
      return config;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read launcher config: " + configPath, exception);
    }
  }

  private Path resolveConfigPath() {
    String configPathFromEnv = System.getenv(CONFIG_PATH_ENV);
    if (configPathFromEnv == null || configPathFromEnv.isBlank()) {
      return Path.of(DEFAULT_CONFIG_PATH);
    }
    return Path.of(configPathFromEnv.trim());
  }
}
