package net.spookly.kodama.brain.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.brain.config.PluginsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class BrainPluginRegistry implements DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(BrainPluginRegistry.class);
  private static final TypeReference<Map<String, String>> VARIABLES_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final List<URLClassLoader> classLoaders = new ArrayList<>();
  private final List<KodamaBrainPlugin> enabledPlugins;

  public BrainPluginRegistry(PluginsProperties properties, ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.enabledPlugins = List.copyOf(loadEnabledPlugins(properties));
  }

  public BrainPrepareInstanceVariables resolvePrepareInstanceVariables(
      BrainPrepareInstanceContext context, Map<String, String> variables, String variablesJson) {
    if (enabledPlugins.isEmpty()) {
      return new BrainPrepareInstanceVariables(variables, variablesJson);
    }
    Map<String, String> mergedVariables = variables == null ? null : new LinkedHashMap<>(variables);
    String mergedVariablesJson = variablesJson;
    boolean mutated = false;

    for (KodamaBrainPlugin plugin : enabledPlugins) {
      BrainPrepareInstanceMutation mutation = invokePrepareHook(plugin, context);
      if (mutation == null || mutation.isEmpty()) {
        continue;
      }
      if (mergedVariables == null) {
        mergedVariables = parseVariablesJson(mergedVariablesJson);
        mergedVariablesJson = null;
      }
      applyMutation(plugin, mergedVariables, mutation);
      mutated = true;
    }

    if (!mutated) {
      return new BrainPrepareInstanceVariables(variables, variablesJson);
    }
    return new BrainPrepareInstanceVariables(mergedVariables, null);
  }

  public List<KodamaBrainPlugin> getEnabledPlugins() {
    return enabledPlugins;
  }

  @Override
  public void destroy() {
    for (URLClassLoader classLoader : classLoaders) {
      closeClassLoader(classLoader);
    }
  }

  private BrainPrepareInstanceMutation invokePrepareHook(
      KodamaBrainPlugin plugin, BrainPrepareInstanceContext context) {
    try {
      return plugin.onPrepareInstance(context);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Plugin " + plugin.id() + " failed during onPrepareInstance", ex);
    }
  }

  private void applyMutation(
      KodamaBrainPlugin plugin,
      Map<String, String> variables,
      BrainPrepareInstanceMutation mutation) {
    for (String key : mutation.variablesToRemove()) {
      if (key == null || key.isBlank()) {
        throw new IllegalStateException(
            "Plugin " + plugin.id() + " returned a blank variable name to remove");
      }
      variables.remove(key);
    }
    for (Map.Entry<String, String> entry : mutation.variablesToSet().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key == null || key.isBlank()) {
        throw new IllegalStateException(
            "Plugin " + plugin.id() + " returned a blank variable name to set");
      }
      if (value == null) {
        throw new IllegalStateException(
            "Plugin " + plugin.id() + " returned a null value for variable " + key);
      }
      variables.put(key, value);
    }
  }

  private Map<String, String> parseVariablesJson(String variablesJson) {
    if (variablesJson == null || variablesJson.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      Map<String, String> parsed = objectMapper.readValue(variablesJson, VARIABLES_TYPE);
      if (parsed == null) {
        return new LinkedHashMap<>();
      }
      return new LinkedHashMap<>(parsed);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("variablesJson must be a JSON object with string values", ex);
    }
  }

  private List<KodamaBrainPlugin> loadEnabledPlugins(PluginsProperties properties) {
    if (properties == null) {
      logger.info("Plugin loading skipped: plugins configuration is missing");
      return List.of();
    }
    List<String> enabledIds = properties.getEnabled();
    if (enabledIds.isEmpty()) {
      logger.info("Plugin loading skipped: plugins.enabled is empty");
      return List.of();
    }

    Set<String> uniqueEnabled = new LinkedHashSet<>(enabledIds);
    if (uniqueEnabled.size() != enabledIds.size()) {
      throw new IllegalStateException("plugins.enabled contains duplicate entries");
    }

    Path pluginsDir = Path.of(properties.getDir()).toAbsolutePath().normalize();
    if (!Files.exists(pluginsDir)) {
      throw new IllegalStateException("Plugins directory does not exist at " + pluginsDir);
    }
    if (!Files.isDirectory(pluginsDir)) {
      throw new IllegalStateException("Plugins path is not a directory: " + pluginsDir);
    }

    Map<String, KodamaBrainPlugin> pluginsById = new LinkedHashMap<>();
    List<Throwable> jarFailures = new ArrayList<>();
    List<Path> pluginJars = listPluginJars(pluginsDir);
    if (pluginJars.isEmpty()) {
      throw new IllegalStateException("No plugin jars found in " + pluginsDir);
    }

    ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
    for (Path jar : pluginJars) {
      loadJarPlugins(jar, parentClassLoader, uniqueEnabled, pluginsById, jarFailures);
    }

    List<KodamaBrainPlugin> enabledPlugins = new ArrayList<>();
    for (String enabledId : enabledIds) {
      KodamaBrainPlugin plugin = pluginsById.get(enabledId);
      if (plugin == null) {
        throw buildMissingPluginException(enabledId, jarFailures);
      }
      enabledPlugins.add(plugin);
    }

    logger.info(
        "Loaded {} plugin(s): {}",
        enabledPlugins.size(),
        enabledPlugins.stream().map(KodamaBrainPlugin::id).toList());
    return enabledPlugins;
  }

  private List<Path> listPluginJars(Path pluginsDir) {
    try (Stream<Path> stream = Files.list(pluginsDir)) {
      return stream.filter(path -> path.toString().endsWith(".jar")).sorted().toList();
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to list plugins directory " + pluginsDir, ex);
    }
  }

  private void loadJarPlugins(
      Path jar,
      ClassLoader parentClassLoader,
      Set<String> enabledIds,
      Map<String, KodamaBrainPlugin> pluginsById,
      List<Throwable> jarFailures) {
    URLClassLoader classLoader;
    try {
      classLoader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, parentClassLoader);
    } catch (IOException ex) {
      jarFailures.add(
          new IllegalStateException("Failed to create plugin classloader for " + jar, ex));
      return;
    }

    boolean keepClassLoader = false;
    ServiceLoader<KodamaBrainPlugin> loader =
        ServiceLoader.load(KodamaBrainPlugin.class, classLoader);
    try {
      for (KodamaBrainPlugin plugin : loader) {
        if (registerPlugin(jar, plugin, enabledIds, pluginsById)) {
          keepClassLoader = true;
        }
      }
    } catch (ServiceConfigurationError error) {
      jarFailures.add(new IllegalStateException("Failed to load plugins from " + jar, error));
    } finally {
      if (keepClassLoader) {
        classLoaders.add(classLoader);
      } else {
        closeClassLoader(classLoader);
      }
    }
  }

  private boolean registerPlugin(
      Path jar,
      KodamaBrainPlugin plugin,
      Set<String> enabledIds,
      Map<String, KodamaBrainPlugin> pluginsById) {
    if (plugin == null) {
      return false;
    }
    String id;
    try {
      id = plugin.id();
    } catch (RuntimeException ex) {
      logger.debug("Skipping plugin in {} that failed to provide an id", jar.getFileName(), ex);
      return false;
    }
    if (id == null || id.isBlank()) {
      logger.debug("Skipping plugin in {} with blank id", jar.getFileName());
      return false;
    }
    if (!enabledIds.contains(id)) {
      logger.debug("Skipping disabled plugin {} from {}", id, jar.getFileName());
      return false;
    }
    if (plugin.apiVersion() != KodamaBrainPlugin.API_VERSION) {
      throw new IllegalStateException(
          "Plugin "
              + id
              + " uses apiVersion "
              + plugin.apiVersion()
              + ", expected "
              + KodamaBrainPlugin.API_VERSION);
    }
    if (pluginsById.containsKey(id)) {
      throw new IllegalStateException("Duplicate plugin id " + id + " found in " + jar);
    }
    pluginsById.put(id, plugin);
    logger.info("Loaded enabled plugin {} from {}", id, jar.getFileName());
    return true;
  }

  private IllegalStateException buildMissingPluginException(
      String enabledId, List<Throwable> jarFailures) {
    IllegalStateException exception =
        new IllegalStateException("Enabled plugin not found: " + enabledId);
    for (Throwable failure : jarFailures) {
      exception.addSuppressed(failure);
    }
    return exception;
  }

  private void closeClassLoader(URLClassLoader classLoader) {
    try {
      classLoader.close();
    } catch (IOException ex) {
      logger.warn("Failed to close plugin classloader", ex);
    }
  }
}
