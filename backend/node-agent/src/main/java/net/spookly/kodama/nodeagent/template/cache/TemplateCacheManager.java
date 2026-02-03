package net.spookly.kodama.nodeagent.template.cache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheManager.class);

    private final TemplateCacheLayout layout;

    public TemplateCacheManager(TemplateCacheLayout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public TemplateCachePurgeResult purgeAll() {
        Path templatesRoot = layout.getTemplatesRoot();
        ensureTemplatesRoot(templatesRoot);
        TemplateCachePurgeResult total = new TemplateCachePurgeResult(0, 0, 0);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesRoot)) {
            for (Path child : stream) {
                ensureWithinTemplatesRoot(templatesRoot, child);
                TemplateCachePurgeResult result = deleteRecursively(child);
                total = total.add(result);
            }
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to purge template cache under " + templatesRoot, ex);
        }
        logger.info(
                "Template cache purge completed. scope=all removedFiles={} removedDirectories={} removedBytes={}",
                total.deletedFiles(),
                total.deletedDirectories(),
                total.deletedBytes()
        );
        return total;
    }

    public TemplateCachePurgeResult purgeTemplate(String templateId) {
        String normalizedTemplateId = requireTemplateId(templateId);
        Path templatesRoot = layout.getTemplatesRoot();
        ensureTemplatesRoot(templatesRoot);
        Path templateRoot = layout.resolveTemplateRoot(normalizedTemplateId);
        ensureWithinTemplatesRoot(templatesRoot, templateRoot);
        TemplateCachePurgeResult result = deleteRecursively(templateRoot);
        logger.info(
                "Template cache purge completed. scope=template templateId={} removedFiles={} removedDirectories={} removedBytes={}",
                normalizedTemplateId,
                result.deletedFiles(),
                result.deletedDirectories(),
                result.deletedBytes()
        );
        return result;
    }

    private void ensureTemplatesRoot(Path templatesRoot) {
        try {
            Files.createDirectories(templatesRoot);
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to ensure template cache directory at " + templatesRoot, ex);
        }
    }

    private TemplateCachePurgeResult deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return new TemplateCachePurgeResult(0, 0, 0);
        }
        ensureWithinTemplatesRoot(layout.getTemplatesRoot(), root);
        PurgeAccumulator accumulator = new PurgeAccumulator();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    long size = attrs == null ? safeSize(file) : attrs.size();
                    Files.deleteIfExists(file);
                    accumulator.addFile(size);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    accumulator.addDirectory();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to delete cache path " + root, ex);
        }
        return accumulator.toResult();
    }

    private long safeSize(Path file) throws IOException {
        return Files.isRegularFile(file) ? Files.size(file) : 0L;
    }

    private void ensureWithinTemplatesRoot(Path templatesRoot, Path candidate) {
        if (templatesRoot == null || candidate == null) {
            throw new TemplateCacheException("Template cache purge path cannot be null");
        }
        Path normalizedRoot = templatesRoot.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new TemplateCacheException(
                    "Refusing to delete path outside template cache root. root=" + normalizedRoot + " path=" + normalizedCandidate
            );
        }
    }

    private String requireTemplateId(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId is required");
        }
        return templateId.trim();
    }

    private static final class PurgeAccumulator {

        private long deletedFiles;
        private long deletedDirectories;
        private long deletedBytes;

        void addFile(long size) {
            deletedFiles++;
            deletedBytes += Math.max(0L, size);
        }

        void addDirectory() {
            deletedDirectories++;
        }

        TemplateCachePurgeResult toResult() {
            return new TemplateCachePurgeResult(deletedFiles, deletedDirectories, deletedBytes);
        }
    }
}
