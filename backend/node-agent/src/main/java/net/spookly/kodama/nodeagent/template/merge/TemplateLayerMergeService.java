package net.spookly.kodama.nodeagent.template.merge;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.io.UncheckedIOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateLayerMergeService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateLayerMergeService.class);

    public void mergeLayers(String instanceId, Path mergedDir, List<TemplateLayerSource> layers) {
        String normalizedInstanceId = requireValue("instanceId", instanceId);
        Path targetDir = Objects.requireNonNull(mergedDir, "mergedDir");
        List<TemplateLayerSource> orderedLayers = normalizeLayers(layers);

        resetDirectory(targetDir, "merged workspace");
        logger.info(
                "Merging template layers into workspace. instanceId={} layers={} path={}",
                normalizedInstanceId,
                orderedLayers.size(),
                targetDir
        );

        for (TemplateLayerSource layer : orderedLayers) {
            logger.info(
                    "Applying template layer orderIndex={} templateId={} version={}",
                    layer.orderIndex(),
                    layer.templateId(),
                    layer.version()
            );
            mergeLayerContents(layer, targetDir);
        }

        logger.info(
                "Template merge complete. instanceId={} layersApplied={}",
                normalizedInstanceId,
                orderedLayers.size()
        );
    }

    private List<TemplateLayerSource> normalizeLayers(List<TemplateLayerSource> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new TemplateLayerMergeException("template layers are required");
        }
        Map<Integer, TemplateLayerSource> byOrder = new HashMap<>();
        for (TemplateLayerSource layer : layers) {
            if (layer == null) {
                throw new TemplateLayerMergeException("template layer is required");
            }
            requireValue("templateId", layer.templateId());
            requireValue("version", layer.version());
            Path contentsDir = Objects.requireNonNull(layer.contentsDir(), "contentsDir");
            if (!Files.isDirectory(contentsDir)) {
                throw new TemplateLayerMergeException("Template contents missing at " + contentsDir);
            }
            if (byOrder.containsKey(layer.orderIndex())) {
                throw new TemplateLayerMergeException("Duplicate template layer orderIndex: " + layer.orderIndex());
            }
            byOrder.put(layer.orderIndex(), layer);
        }
        List<TemplateLayerSource> ordered = new ArrayList<>(byOrder.values());
        ordered.sort(Comparator.comparingInt(TemplateLayerSource::orderIndex));
        return ordered;
    }

    private void mergeLayerContents(TemplateLayerSource layer, Path targetDir) {
        Path sourceDir = layer.contentsDir().toAbsolutePath().normalize();
        try {
            Files.walkFileTree(sourceDir, new MergeFileVisitor(sourceDir, targetDir));
        } catch (IOException ex) {
            throw new TemplateLayerMergeException(
                    "Failed to merge template contents for templateId=" + layer.templateId()
                            + " version=" + layer.version(),
                    ex
            );
        }
    }

    private void resetDirectory(Path dir, String label) {
        try {
            if (Files.exists(dir)) {
                deleteRecursively(dir);
            }
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new TemplateLayerMergeException("Failed to reset " + label + " at " + dir, ex);
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private String requireValue(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new TemplateLayerMergeException(label + " is required");
        }
        return value.trim();
    }

    private static class MergeFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceRoot;
        private final Path targetRoot;

        private MergeFileVisitor(Path sourceRoot, Path targetRoot) {
            this.sourceRoot = sourceRoot;
            this.targetRoot = targetRoot;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetDir = resolveTarget(dir);
            ensureDirectory(targetDir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path targetFile = resolveTarget(file);
            Files.createDirectories(targetFile.getParent());
            ensureFileTarget(targetFile);
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            copyPermissions(file, targetFile);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            Path targetDir = resolveTarget(dir);
            copyPermissions(dir, targetDir);
            return FileVisitResult.CONTINUE;
        }

        private Path resolveTarget(Path sourcePath) {
            Path relative = sourceRoot.relativize(sourcePath);
            Path target = targetRoot.resolve(relative).normalize();
            if (!target.startsWith(targetRoot)) {
                throw new TemplateLayerMergeException("Template entry escapes workspace: " + relative);
            }
            return target;
        }

        private void ensureDirectory(Path targetDir) throws IOException {
            if (Files.exists(targetDir) && !Files.isDirectory(targetDir)) {
                Files.delete(targetDir);
            }
            Files.createDirectories(targetDir);
        }

        private void ensureFileTarget(Path targetFile) throws IOException {
            if (Files.exists(targetFile) && Files.isDirectory(targetFile)) {
                deleteRecursively(targetFile);
            }
        }

        private void deleteRecursively(Path root) throws IOException {
            if (!Files.exists(root)) {
                return;
            }
            try (var stream = Files.walk(root)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ex) {
                                throw new UncheckedIOException(ex);
                            }
                        });
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }

        private void copyPermissions(Path source, Path target) throws IOException {
            PosixFileAttributeView sourceView = Files.getFileAttributeView(source, PosixFileAttributeView.class);
            PosixFileAttributeView targetView = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            if (sourceView != null && targetView != null) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(source);
                Files.setPosixFilePermissions(target, permissions);
                return;
            }
            if (Files.isExecutable(source)) {
                target.toFile().setExecutable(true, false);
            }
        }
    }
}
