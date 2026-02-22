package net.spookly.kodama.nodelauncher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

public final class ConfigLoader {

  static final String CONFIG_PATH_ENV = "KODAMA_LAUNCHER_CONFIG";
  private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
  private static final String DEFAULT_CONFIG_PATH = "./config.yml";
  private static final String DEFAULT_CONFIG_TEMPLATE =
      """
      github:
        owner: "Spookly-Network"
        repo: "Kodama"
        channel: stable
        assetRegex: "kodama-node-agent-(.*)\\\\.jar"
      verify:
        sha256Required: true
        sha256Suffix: ".sha256"
      installDir: "/opt/kodama-node"
      javaBin: "java"
      agentArgs:
        - "--brainUrl=http://localhost:8080"
      updateMode: "NEXT_START"
      """;

  private final ObjectMapper objectMapper;
  private final Function<String, String> environmentLookup;
  private final Path defaultConfigPath;

  public ConfigLoader() {
    this(System::getenv, Path.of(DEFAULT_CONFIG_PATH));
  }

  ConfigLoader(Function<String, String> environmentLookup, Path defaultConfigPath) {
    this.objectMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
    this.environmentLookup = Objects.requireNonNull(environmentLookup, "environmentLookup");
    this.defaultConfigPath = Objects.requireNonNull(defaultConfigPath, "defaultConfigPath");
  }

  public LauncherConfig load() {
    Path configPath = resolveConfigPath();
    ensureConfigFileExists(configPath);
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
    String configPathFromEnv = environmentLookup.apply(CONFIG_PATH_ENV);
    if (configPathFromEnv == null || configPathFromEnv.isBlank()) {
      return defaultConfigPath;
    }
    return Path.of(configPathFromEnv.trim());
  }

  private void ensureConfigFileExists(Path configPath) {
    if (Files.isRegularFile(configPath)) {
      return;
    }
    if (Files.exists(configPath)) {
      throw new IllegalStateException(
          "Launcher config path exists but is not a regular file: " + configPath);
    }

    try {
      Path parent = configPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(
          configPath,
          DEFAULT_CONFIG_TEMPLATE,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
      logger.warning(
          "Launcher config file did not exist. Created default config at "
              + configPath
              + ". Update github.owner and github.repo before relying on updates.");
    } catch (FileAlreadyExistsException ignored) {
      // Another process created the file in parallel. Continue with normal loading.
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to create default launcher config: " + configPath, exception);
    }
  }
}
