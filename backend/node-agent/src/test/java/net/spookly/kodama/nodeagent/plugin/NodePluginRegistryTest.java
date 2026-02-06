package net.spookly.kodama.nodeagent.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import net.spookly.kodama.nodeagent.config.NodePluginsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodePluginRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void ignoresDisabledPluginsWithMismatchedApiVersion() throws Exception {
        Path pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        createJar(pluginsDir.resolve("disabled.jar"), DisabledApiMismatchPlugin.class);
        createJar(pluginsDir.resolve("enabled.jar"), EnabledPlugin.class);

        NodePluginsProperties properties = new NodePluginsProperties();
        properties.setDir(pluginsDir.toString());
        properties.setEnabled(List.of("enabled-plugin"));

        NodePluginRegistry registry = new NodePluginRegistry(properties);
        try {
            List<KodamaNodePlugin> enabled = registry.getEnabledPlugins();
            assertEquals(1, enabled.size());
            assertEquals("enabled-plugin", enabled.get(0).id());
        } finally {
            registry.destroy();
        }
    }

    @Test
    void ignoresDisabledDuplicateIds() throws Exception {
        Path pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        createJar(pluginsDir.resolve("enabled.jar"), EnabledPlugin.class);
        createJar(pluginsDir.resolve("disabled-a.jar"), DisabledDuplicatePluginA.class);
        createJar(pluginsDir.resolve("disabled-b.jar"), DisabledDuplicatePluginB.class);

        NodePluginsProperties properties = new NodePluginsProperties();
        properties.setDir(pluginsDir.toString());
        properties.setEnabled(List.of("enabled-plugin"));

        NodePluginRegistry registry = new NodePluginRegistry(properties);
        try {
            List<KodamaNodePlugin> enabled = registry.getEnabledPlugins();
            assertEquals(1, enabled.size());
            assertEquals("enabled-plugin", enabled.get(0).id());
        } finally {
            registry.destroy();
        }
    }

    private static Path createJar(Path jarPath, Class<?>... pluginClasses) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeServiceEntry(jarOutputStream, KodamaNodePlugin.class, pluginClasses);
            for (Class<?> pluginClass : pluginClasses) {
                writeClassEntry(jarOutputStream, pluginClass);
            }
        }
        return jarPath;
    }

    private static void writeServiceEntry(
            JarOutputStream jarOutputStream,
            Class<?> serviceType,
            Class<?>... pluginClasses
    ) throws IOException {
        JarEntry entry = new JarEntry("META-INF/services/" + serviceType.getName());
        jarOutputStream.putNextEntry(entry);
        String content = Arrays.stream(pluginClasses)
                .map(Class::getName)
                .collect(Collectors.joining("\n"));
        jarOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
    }

    private static void writeClassEntry(JarOutputStream jarOutputStream, Class<?> pluginClass) throws IOException {
        String resourcePath = pluginClass.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = pluginClass.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing class resource " + resourcePath);
            }
            jarOutputStream.putNextEntry(new JarEntry(resourcePath));
            inputStream.transferTo(jarOutputStream);
            jarOutputStream.closeEntry();
        }
    }

    public static final class EnabledPlugin implements KodamaNodePlugin {

        @Override
        public String id() {
            return "enabled-plugin";
        }
    }

    public static final class DisabledApiMismatchPlugin implements KodamaNodePlugin {

        @Override
        public String id() {
            return "disabled-api-mismatch";
        }

        @Override
        public int apiVersion() {
            return KodamaNodePlugin.API_VERSION + 1;
        }
    }

    public static final class DisabledDuplicatePluginA implements KodamaNodePlugin {

        @Override
        public String id() {
            return "disabled-duplicate";
        }
    }

    public static final class DisabledDuplicatePluginB implements KodamaNodePlugin {

        @Override
        public String id() {
            return "disabled-duplicate";
        }
    }
}
