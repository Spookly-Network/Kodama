package net.spookly.kodama.nodeagent.plugin;

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
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import net.spookly.kodama.nodeagent.config.NodePluginsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class NodePluginRegistry implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NodePluginRegistry.class);

    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    private final List<KodamaNodePlugin> enabledPlugins;

    public NodePluginRegistry(NodePluginsProperties properties) {
        this.enabledPlugins = List.copyOf(loadEnabledPlugins(properties));
    }

    public NodeInstanceStartSpec resolveStartSpec(
            NodeInstanceStartContext context,
            Map<String, String> env,
            Map<String, String> labels,
            List<String> command
    ) {
        if (enabledPlugins.isEmpty()) {
            return new NodeInstanceStartSpec(env, labels, command);
        }
        Map<String, String> mergedEnv = env == null ? new LinkedHashMap<>() : new LinkedHashMap<>(env);
        Map<String, String> mergedLabels = labels == null ? new LinkedHashMap<>() : new LinkedHashMap<>(labels);
        List<String> mergedCommand = command == null ? null : new ArrayList<>(command);
        boolean mutated = false;

        for (KodamaNodePlugin plugin : enabledPlugins) {
            NodeInstanceStartMutation mutation = invokeStartHook(plugin, context);
            if (mutation == null || mutation.isEmpty()) {
                continue;
            }
            applyMutation(plugin, mergedEnv, mergedLabels, mutation);
            if (mutation.commandOverride() != null) {
                if (mergedCommand != null) {
                    throw new IllegalStateException(
                            "Plugin " + plugin.id() + " attempted to override command after it was already set"
                    );
                }
                mergedCommand = new ArrayList<>(mutation.commandOverride());
            }
            mutated = true;
        }

        if (!mutated) {
            return new NodeInstanceStartSpec(env, labels, command);
        }
        return new NodeInstanceStartSpec(mergedEnv, mergedLabels, mergedCommand);
    }

    public List<KodamaNodePlugin> getEnabledPlugins() {
        return enabledPlugins;
    }

    @Override
    public void destroy() {
        for (URLClassLoader classLoader : classLoaders) {
            closeClassLoader(classLoader);
        }
    }

    private NodeInstanceStartMutation invokeStartHook(KodamaNodePlugin plugin, NodeInstanceStartContext context) {
        try {
            return plugin.onBeforeInstanceStart(context);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Plugin " + plugin.id() + " failed during onBeforeInstanceStart",
                    ex
            );
        }
    }

    private void applyMutation(
            KodamaNodePlugin plugin,
            Map<String, String> env,
            Map<String, String> labels,
            NodeInstanceStartMutation mutation
    ) {
        for (String key : mutation.envToRemove()) {
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a blank env name to remove");
            }
            env.remove(key);
        }
        for (Map.Entry<String, String> entry : mutation.envToSet().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a blank env name to set");
            }
            if (value == null) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a null env value for " + key);
            }
            env.put(key, value);
        }
        for (String key : mutation.labelsToRemove()) {
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a blank label name to remove");
            }
            labels.remove(key);
        }
        for (Map.Entry<String, String> entry : mutation.labelsToSet().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a blank label name to set");
            }
            if (value == null) {
                throw new IllegalStateException("Plugin " + plugin.id() + " returned a null label value for " + key);
            }
            labels.put(key, value);
        }
    }

    private List<KodamaNodePlugin> loadEnabledPlugins(NodePluginsProperties properties) {
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

        Map<String, KodamaNodePlugin> pluginsById = new LinkedHashMap<>();
        List<Throwable> jarFailures = new ArrayList<>();
        List<Path> pluginJars = listPluginJars(pluginsDir);
        if (pluginJars.isEmpty()) {
            throw new IllegalStateException("No plugin jars found in " + pluginsDir);
        }

        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        for (Path jar : pluginJars) {
            loadJarPlugins(jar, parentClassLoader, uniqueEnabled, pluginsById, jarFailures);
        }

        List<KodamaNodePlugin> enabledPlugins = new ArrayList<>();
        for (String enabledId : enabledIds) {
            KodamaNodePlugin plugin = pluginsById.get(enabledId);
            if (plugin == null) {
                throw buildMissingPluginException(enabledId, jarFailures);
            }
            enabledPlugins.add(plugin);
        }

        logger.info("Loaded {} plugin(s): {}", enabledPlugins.size(), enabledPlugins.stream().map(KodamaNodePlugin::id).toList());
        return enabledPlugins;
    }

    private List<Path> listPluginJars(Path pluginsDir) {
        try (Stream<Path> stream = Files.list(pluginsDir)) {
            return stream
                    .filter(path -> path.toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list plugins directory " + pluginsDir, ex);
        }
    }

    private void loadJarPlugins(
            Path jar,
            ClassLoader parentClassLoader,
            Set<String> enabledIds,
            Map<String, KodamaNodePlugin> pluginsById,
            List<Throwable> jarFailures
    ) {
        URLClassLoader classLoader;
        try {
            classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, parentClassLoader);
        } catch (IOException ex) {
            jarFailures.add(new IllegalStateException("Failed to create plugin classloader for " + jar, ex));
            return;
        }

        boolean keepClassLoader = false;
        ServiceLoader<KodamaNodePlugin> loader = ServiceLoader.load(KodamaNodePlugin.class, classLoader);
        try {
            for (KodamaNodePlugin plugin : loader) {
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
            KodamaNodePlugin plugin,
            Set<String> enabledIds,
            Map<String, KodamaNodePlugin> pluginsById
    ) {
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
        if (plugin.apiVersion() != KodamaNodePlugin.API_VERSION) {
            throw new IllegalStateException(
                    "Plugin " + id + " uses apiVersion " + plugin.apiVersion()
                            + ", expected " + KodamaNodePlugin.API_VERSION
            );
        }
        if (pluginsById.containsKey(id)) {
            throw new IllegalStateException("Duplicate plugin id " + id + " found in " + jar);
        }
        pluginsById.put(id, plugin);
        logger.info("Loaded enabled plugin {} from {}", id, jar.getFileName());
        return true;
    }

    private IllegalStateException buildMissingPluginException(String enabledId, List<Throwable> jarFailures) {
        IllegalStateException exception = new IllegalStateException("Enabled plugin not found: " + enabledId);
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
