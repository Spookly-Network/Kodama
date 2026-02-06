package net.spookly.kodama.nodeagent.template.cache;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.stereotype.Component;

@Component
public class TemplateCacheLayout {

    public static final String TEMPLATES_DIR_NAME = "templates";
    public static final String CONTENTS_DIR_NAME = "contents";
    public static final String CHECKSUM_FILENAME = "checksum.sha256";
    public static final String METADATA_FILENAME = "metadata.json";

    private final Path cacheRoot;
    private final Path templatesRoot;

    public TemplateCacheLayout(NodeConfig config) {
        if (config == null || config.getCacheDir() == null || config.getCacheDir().isBlank()) {
            throw new TemplateCacheException("node-agent.cache-dir is required to build template cache layout");
        }
        String cacheDir = config.getCacheDir().trim();
        try {
            this.cacheRoot = Paths.get(cacheDir).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new TemplateCacheException("Invalid node-agent.cache-dir: " + cacheDir, ex);
        }
        this.templatesRoot = cacheRoot.resolve(TEMPLATES_DIR_NAME);
    }

    public Path getCacheRoot() {
        return cacheRoot;
    }

    public Path getTemplatesRoot() {
        return templatesRoot;
    }

    public Path resolveTemplateRoot(String templateId) {
        String normalizedTemplateId = requireSegment("templateId", templateId);
        return templatesRoot.resolve(normalizedTemplateId);
    }

    public TemplateCachePaths resolveTemplateVersion(String templateId, String version) {
        String normalizedTemplateId = requireSegment("templateId", templateId);
        String normalizedVersion = requireSegment("version", version);
        Path templateRoot = templatesRoot.resolve(normalizedTemplateId);
        Path versionRoot = templateRoot.resolve(normalizedVersion);
        Path contentsDir = versionRoot.resolve(CONTENTS_DIR_NAME);
        Path checksumFile = versionRoot.resolve(CHECKSUM_FILENAME);
        Path metadataFile = versionRoot.resolve(METADATA_FILENAME);
        return new TemplateCachePaths(
                normalizedTemplateId,
                normalizedVersion,
                templateRoot,
                versionRoot,
                contentsDir,
                checksumFile,
                metadataFile
        );
    }

    private String requireSegment(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        String trimmed = value.trim();
        Path segment;
        try {
            segment = Paths.get(trimmed);
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException(label + " contains invalid path characters", ex);
        }
        if (segment.isAbsolute() || segment.getNameCount() != 1) {
            throw new IllegalArgumentException(label + " must be a single path segment");
        }
        String name = segment.getFileName().toString();
        if (".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException(label + " must not be '.' or '..'");
        }
        return name;
    }
}
