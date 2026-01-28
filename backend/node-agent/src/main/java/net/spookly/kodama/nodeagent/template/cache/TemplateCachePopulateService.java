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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import net.spookly.kodama.nodeagent.config.NodeConfig;
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
    private final NodeConfig.TemplateCacheLimits cacheLimits;

    public TemplateCachePopulateService(
            TemplateCacheLayout layout,
            TemplateCacheLookupService lookupService,
            TemplateStorageClient storageClient,
            ObjectMapper objectMapper,
            NodeConfig config
    ) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.lookupService = Objects.requireNonNull(lookupService, "lookupService");
        this.storageClient = Objects.requireNonNull(storageClient, "storageClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.cacheLimits = Objects.requireNonNull(config, "config").getTemplateCacheLimits();
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

            DownloadResult downloadResult = downloadTarball(paths, s3Key, tarballFile);
            validateDownload(paths, downloadResult, checksum);
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

            if (moved) {
                logger.info(
                        "Template cache populated. templateId={}, version={}, checksum={}",
                        paths.templateId(),
                        paths.version(),
                        checksum
                );
            }
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

    private DownloadResult downloadTarball(TemplateCachePaths paths, String s3Key, Path tarballFile) throws IOException {
        try (TemplateTarball tarball = storageClient.getTemplateTarball(paths.templateId(), paths.version(), s3Key);
             InputStream inputStream = tarball.getInputStream();
             OutputStream outputStream = new BufferedOutputStream(
                     Files.newOutputStream(tarballFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
             )) {
            MessageDigest digest = createSha256Digest();
            try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                long bytesWritten = digestInputStream.transferTo(outputStream);
                String checksum = toHexLower(digest.digest());
                return new DownloadResult(checksum, bytesWritten, tarball.getContentLength());
            }
        }
    }

    private void validateDownload(TemplateCachePaths paths, DownloadResult downloadResult, String expectedChecksum) {
        if (downloadResult.contentLength() > 0 && downloadResult.bytesWritten() != downloadResult.contentLength()) {
            throw new TemplateCacheException(
                    "Template tarball length mismatch for templateId=" + paths.templateId()
                            + " version=" + paths.version()
                            + " expected=" + downloadResult.contentLength()
                            + " actual=" + downloadResult.bytesWritten()
            );
        }
        String normalizedExpected = expectedChecksum.trim().toLowerCase(Locale.ROOT);
        if (!downloadResult.checksum().equals(normalizedExpected)) {
            throw new TemplateCacheException(
                    "Template tarball checksum mismatch for templateId=" + paths.templateId()
                            + " version=" + paths.version()
            );
        }
    }

    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new TemplateCacheException("SHA-256 digest is not available", ex);
        }
    }

    private String toHexLower(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private void extractTarball(String s3Key, Path tarballFile, Path destinationDir) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(tarballFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             InputStream archiveStream = wrapIfGzip(s3Key, bufferedInputStream);
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(archiveStream)) {

            long maxExtractedBytes = cacheLimits.getMaxExtractedBytes();
            long maxEntries = cacheLimits.getMaxEntries();
            long extractedBytes = 0;
            long entryCount = 0;
            Map<Path, Integer> directoryModes = new HashMap<>();

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                entryCount++;
                if (entryCount > maxEntries) {
                    throw new TemplateCacheException("Template tarball exceeds max entry count of " + maxEntries);
                }
                if (entry.isDirectory()) {
                    Path dirPath = resolveEntryPath(destinationDir, entry.getName());
                    Files.createDirectories(dirPath);
                    directoryModes.put(dirPath, entry.getMode());
                    continue;
                }
                if (entry.isSymbolicLink() || entry.isLink()) {
                    throw new TemplateCacheException("Template tarball contains unsupported link entry: " + entry.getName());
                }
                Path entryPath = resolveEntryPath(destinationDir, entry.getName());
                Files.createDirectories(entryPath.getParent());
                long entrySize = entry.getSize();
                if (entrySize > 0 && extractedBytes + entrySize > maxExtractedBytes) {
                    throw new TemplateCacheException(
                            "Template tarball exceeds max extracted bytes of " + maxExtractedBytes
                                    + " while extracting " + entry.getName()
                    );
                }
                try (OutputStream outputStream = new BufferedOutputStream(
                        Files.newOutputStream(entryPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                )) {
                    extractedBytes = copyEntryWithLimit(
                            tarInputStream,
                            outputStream,
                            extractedBytes,
                            maxExtractedBytes,
                            entry.getName()
                    );
                }
                applyPermissions(entryPath, entry.getMode());
            }

            applyDirectoryPermissions(directoryModes);
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

    private long copyEntryWithLimit(
            TarArchiveInputStream tarInputStream,
            OutputStream outputStream,
            long extractedBytes,
            long maxExtractedBytes,
            String entryName
    ) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = tarInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
            extractedBytes += read;
            if (extractedBytes > maxExtractedBytes) {
                throw new TemplateCacheException(
                        "Template tarball exceeds max extracted bytes of " + maxExtractedBytes
                                + " while extracting " + entryName
                );
            }
        }
        return extractedBytes;
    }

    private void applyDirectoryPermissions(Map<Path, Integer> directoryModes) throws IOException {
        if (directoryModes.isEmpty()) {
            return;
        }
        List<Map.Entry<Path, Integer>> entries = new ArrayList<>(directoryModes.entrySet());
        entries.sort(Comparator.<Map.Entry<Path, Integer>>comparingInt(entry -> entry.getKey().getNameCount()).reversed());
        for (Map.Entry<Path, Integer> entry : entries) {
            applyPermissions(entry.getKey(), entry.getValue());
        }
    }

    private void applyPermissions(Path entryPath, int mode) throws IOException {
        PosixFileAttributeView attributeView = Files.getFileAttributeView(entryPath, PosixFileAttributeView.class);
        if (attributeView != null) {
            Files.setPosixFilePermissions(entryPath, toPosixPermissions(mode));
            return;
        }
        applyExecutableFallback(entryPath, mode);
    }

    private Set<PosixFilePermission> toPosixPermissions(int mode) {
        int permissionBits = mode & 0777;
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        if ((permissionBits & 0400) != 0) {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        if ((permissionBits & 0200) != 0) {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((permissionBits & 0100) != 0) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((permissionBits & 0040) != 0) {
            permissions.add(PosixFilePermission.GROUP_READ);
        }
        if ((permissionBits & 0020) != 0) {
            permissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((permissionBits & 0010) != 0) {
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((permissionBits & 0004) != 0) {
            permissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ((permissionBits & 0002) != 0) {
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((permissionBits & 0001) != 0) {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return permissions;
    }

    private void applyExecutableFallback(Path entryPath, int mode) {
        boolean ownerExecutable = (mode & 0100) != 0;
        boolean groupExecutable = (mode & 0010) != 0;
        boolean otherExecutable = (mode & 0001) != 0;
        if (ownerExecutable || groupExecutable || otherExecutable) {
            boolean ownerOnly = !(groupExecutable || otherExecutable);
            entryPath.toFile().setExecutable(true, ownerOnly);
        }
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

    private record DownloadResult(String checksum, long bytesWritten, long contentLength) {
    }
}
