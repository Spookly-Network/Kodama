package net.spookly.kodama.nodeagent.template.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageClient;
import net.spookly.kodama.nodeagent.template.storage.TemplateTarball;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateCachePopulateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCachePopulateService.class);

    private final TemplateCacheLayout layout;
    private final TemplateCacheLookupService lookupService;
    private final TemplateStorageClient storageClient;
    private final ObjectMapper objectMapper;

    public TemplateCachePopulateService(
            TemplateCacheLayout layout,
            TemplateCacheLookupService lookupService,
            TemplateStorageClient storageClient,
            ObjectMapper objectMapper
    ) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.lookupService = Objects.requireNonNull(lookupService, "lookupService");
        this.storageClient = Objects.requireNonNull(storageClient, "storageClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public TemplateCacheLookupResult ensureCachedTemplate(
            String templateId,
            String version,
            String checksum,
            String s3Key
    ) {
        String normalizedChecksum = requireValue("checksum", checksum);
        String normalizedS3Key = requireValue("s3Key", s3Key);
        TemplateCacheLookupResult existing = lookupService.findCachedTemplate(templateId, version, normalizedChecksum);
        if (existing.isCacheHit()) {
            return existing;
        }

        TemplateCachePaths paths = layout.resolveTemplateVersion(templateId, version);
        deleteExistingCache(paths);
        populateCache(paths, normalizedChecksum, normalizedS3Key);

        TemplateCacheLookupResult result = lookupService.findCachedTemplate(templateId, version, normalizedChecksum);
        if (!result.isCacheHit()) {
            throw new TemplateCacheException(
                    "Template cache population failed for templateId=" + paths.templateId() + " version=" + paths.version()
            );
        }
        return result;
    }

    private void populateCache(TemplateCachePaths paths, String checksum, String s3Key) {
        try {
            Files.createDirectories(paths.templateRoot());
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to create template cache root at " + paths.templateRoot(), ex);
        }

        Path tarballFile = null;
        Path tempVersionRoot = null;
        boolean moved = false;
        try {
            tarballFile = Files.createTempFile(paths.templateRoot(), "template-", ".tar");
            tempVersionRoot = Files.createTempDirectory(paths.templateRoot(), ".cache-");
            Path tempContentsDir = tempVersionRoot.resolve(TemplateCacheLayout.CONTENTS_DIR_NAME);
            Files.createDirectories(tempContentsDir);

            downloadTarball(paths, s3Key, tarballFile);
            extractTarball(s3Key, tarballFile, tempContentsDir);

            writeChecksum(tempVersionRoot.resolve(TemplateCacheLayout.CHECKSUM_FILENAME), checksum);
            writeMetadata(
                    tempVersionRoot.resolve(TemplateCacheLayout.METADATA_FILENAME),
                    new TemplateCacheMetadata(paths.templateId(), paths.version(), checksum, s3Key, OffsetDateTime.now())
            );

            try {
                moveToFinalLocation(tempVersionRoot, paths.versionRoot());
                moved = true;
            } catch (FileAlreadyExistsException ex) {
                logger.info(
                        "Template cache already populated by another worker. templateId={}, version={}",
                        paths.templateId(),
                        paths.version()
                );
            }

            logger.info(
                    "Template cache populated. templateId={}, version={}, checksum={}",
                    paths.templateId(),
                    paths.version(),
                    checksum
            );
        } catch (IOException ex) {
            throw new TemplateCacheException(
                    "Failed to populate template cache for templateId=" + paths.templateId() + " version=" + paths.version(),
                    ex
            );
        } finally {
            deleteIfExists(tarballFile);
            if (!moved) {
                deleteRecursively(tempVersionRoot);
            }
        }
    }

    private void downloadTarball(TemplateCachePaths paths, String s3Key, Path tarballFile) throws IOException {
        try (TemplateTarball tarball = storageClient.getTemplateTarball(paths.templateId(), paths.version(), s3Key);
             InputStream inputStream = tarball.getInputStream();
             OutputStream outputStream = new BufferedOutputStream(
                     Files.newOutputStream(tarballFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
             )) {
            inputStream.transferTo(outputStream);
        }
    }

    private void extractTarball(String s3Key, Path tarballFile, Path destinationDir) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(tarballFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             InputStream archiveStream = wrapIfGzip(s3Key, bufferedInputStream);
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(archiveStream)) {

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    Path dirPath = resolveEntryPath(destinationDir, entry.getName());
                    Files.createDirectories(dirPath);
                    continue;
                }
                if (entry.isSymbolicLink() || entry.isLink()) {
                    throw new TemplateCacheException("Template tarball contains unsupported link entry: " + entry.getName());
                }
                Path entryPath = resolveEntryPath(destinationDir, entry.getName());
                Files.createDirectories(entryPath.getParent());
                try (OutputStream outputStream = new BufferedOutputStream(
                        Files.newOutputStream(entryPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                )) {
                    tarInputStream.transferTo(outputStream);
                }
                if (isOwnerExecutable(entry)) {
                    entryPath.toFile().setExecutable(true, true);
                }
            }
        }
    }

    private InputStream wrapIfGzip(String s3Key, BufferedInputStream bufferedInputStream) throws IOException {
        if (isGzipTarball(s3Key, bufferedInputStream)) {
            return new GzipCompressorInputStream(bufferedInputStream);
        }
        return bufferedInputStream;
    }

    private boolean isGzipTarball(String s3Key, BufferedInputStream bufferedInputStream) throws IOException {
        String lowerKey = s3Key.toLowerCase(Locale.ROOT);
        if (lowerKey.endsWith(".tar.gz") || lowerKey.endsWith(".tgz") || lowerKey.endsWith(".gz")) {
            return true;
        }
        if (!bufferedInputStream.markSupported()) {
            return false;
        }
        bufferedInputStream.mark(2);
        int first = bufferedInputStream.read();
        int second = bufferedInputStream.read();
        bufferedInputStream.reset();
        return first == 0x1f && second == 0x8b;
    }

    private Path resolveEntryPath(Path destinationDir, String entryName) {
        if (entryName == null || entryName.isBlank()) {
            throw new TemplateCacheException("Template tarball contains empty entry name");
        }
        Path resolved = destinationDir.resolve(entryName).normalize();
        if (!resolved.startsWith(destinationDir)) {
            throw new TemplateCacheException("Template tarball entry escapes destination: " + entryName);
        }
        return resolved;
    }

    private boolean isOwnerExecutable(TarArchiveEntry entry) {
        return (entry.getMode() & 0100) != 0;
    }

    private void moveToFinalLocation(Path tempVersionRoot, Path finalVersionRoot) throws IOException {
        try {
            Files.move(tempVersionRoot, finalVersionRoot, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            throw new TemplateCacheException(
                    "Atomic move not supported for template cache from " + tempVersionRoot + " to " + finalVersionRoot,
                    ex
            );
        }
    }

    private void writeChecksum(Path checksumFile, String checksum) throws IOException {
        Files.writeString(
                checksumFile,
                checksum,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void writeMetadata(Path metadataFile, TemplateCacheMetadata metadata) throws IOException {
        objectMapper.writeValue(metadataFile.toFile(), metadata);
    }

    private void deleteExistingCache(TemplateCachePaths paths) {
        if (Files.exists(paths.versionRoot())) {
            deleteRecursively(paths.versionRoot());
        }
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            try (var stream = Files.walk(root)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(this::deleteIfExists);
            }
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to delete cache path " + root, ex);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.warn("Failed to delete temp cache path {}", path, ex);
        }
    }

    private String requireValue(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record TemplateCacheMetadata(
            String templateId,
            String version,
            String checksum,
            String s3Key,
            OffsetDateTime cachedAt
    ) {
    }
}
