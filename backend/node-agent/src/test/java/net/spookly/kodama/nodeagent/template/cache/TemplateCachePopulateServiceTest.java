package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageClient;
import net.spookly.kodama.nodeagent.template.storage.TemplateTarball;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateCachePopulateServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void populatesCacheFromTarball() throws Exception {
        byte[] tarballBytes = createTarball(Map.of(
                "server.properties", "motd=hello",
                "config/settings.json", "{\"mode\":\"test\"}"
        ));
        String checksum = sha256Hex(tarballBytes);
        InMemoryTemplateStorageClient storageClient = new InMemoryTemplateStorageClient(tarballBytes);
        NodeConfig config = createConfig();
        TemplateCacheLayout layout = createLayout(config);
        TemplateCachePopulateService service = createService(storageClient, layout, config);

        TemplateCacheLookupResult result = service.ensureCachedTemplate(
                "starter",
                "1.2.3",
                checksum,
                "templates/starter/1.2.3.tar"
        );

        assertThat(result.isCacheHit()).isTrue();
        assertThat(Files.readString(result.contentsDir().resolve("server.properties")))
                .isEqualTo("motd=hello");
        assertThat(Files.readString(result.contentsDir().resolve("config/settings.json")))
                .isEqualTo("{\"mode\":\"test\"}");

        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");
        assertThat(Files.readString(paths.checksumFile())).isEqualTo(checksum);
        assertThat(Files.exists(paths.metadataFile())).isTrue();
        assertThat(storageClient.getFetchCount()).isEqualTo(1);

        TemplateCacheLookupResult second = service.ensureCachedTemplate(
                "starter",
                "1.2.3",
                checksum,
                "templates/starter/1.2.3.tar"
        );
        assertThat(second.isCacheHit()).isTrue();
        assertThat(storageClient.getFetchCount()).isEqualTo(1);
    }

    @Test
    void preservesExecutablePermissionsFromTarball() throws Exception {
        Assumptions.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        byte[] tarballBytes = createTarballWithModes(Map.of(
                "bin/start.sh", new TarEntrySpec("#!/bin/sh\necho ok\n", 0755)
        ));
        String checksum = sha256Hex(tarballBytes);
        InMemoryTemplateStorageClient storageClient = new InMemoryTemplateStorageClient(tarballBytes);
        NodeConfig config = createConfig();
        TemplateCacheLayout layout = createLayout(config);
        TemplateCachePopulateService service = createService(storageClient, layout, config);

        TemplateCacheLookupResult result = service.ensureCachedTemplate(
                "starter",
                "1.2.4",
                checksum,
                "templates/starter/1.2.4.tar"
        );

        Path scriptPath = result.contentsDir().resolve("bin/start.sh");
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(scriptPath);
        assertThat(permissions).contains(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE
        );
    }

    @Test
    void rejectsTarballExceedingMaxExtractedBytes() throws Exception {
        byte[] tarballBytes = createTarball(Map.of(
                "server.properties", "motd=hello"
        ));
        String checksum = sha256Hex(tarballBytes);
        InMemoryTemplateStorageClient storageClient = new InMemoryTemplateStorageClient(tarballBytes);
        NodeConfig config = createConfig();
        config.getTemplateCacheLimits().setMaxExtractedBytes(4);
        TemplateCacheLayout layout = createLayout(config);
        TemplateCachePopulateService service = createService(storageClient, layout, config);

        assertThatThrownBy(() -> service.ensureCachedTemplate(
                "starter",
                "1.2.5",
                checksum,
                "templates/starter/1.2.5.tar"
        )).isInstanceOf(TemplateCacheException.class)
                .hasMessageContaining("max extracted bytes");
    }

    @Test
    void rejectsTarballExceedingMaxEntries() throws Exception {
        byte[] tarballBytes = createTarball(Map.of(
                "one.txt", "1",
                "two.txt", "2",
                "three.txt", "3"
        ));
        String checksum = sha256Hex(tarballBytes);
        InMemoryTemplateStorageClient storageClient = new InMemoryTemplateStorageClient(tarballBytes);
        NodeConfig config = createConfig();
        config.getTemplateCacheLimits().setMaxEntries(2);
        TemplateCacheLayout layout = createLayout(config);
        TemplateCachePopulateService service = createService(storageClient, layout, config);

        assertThatThrownBy(() -> service.ensureCachedTemplate(
                "starter",
                "1.2.6",
                checksum,
                "templates/starter/1.2.6.tar"
        )).isInstanceOf(TemplateCacheException.class)
                .hasMessageContaining("max entry count");
    }

    private TemplateCachePopulateService createService(
            TemplateStorageClient storageClient,
            TemplateCacheLayout layout,
            NodeConfig config
    ) {
        TemplateCacheLookupService lookupService = new TemplateCacheLookupService(layout);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new TemplateCachePopulateService(layout, lookupService, storageClient, mapper, config);
    }

    private NodeConfig createConfig() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());
        return config;
    }

    private TemplateCacheLayout createLayout(NodeConfig config) {
        return new TemplateCacheLayout(config);
    }

    private byte[] createTarball(Map<String, String> files) throws IOException {
        Map<String, TarEntrySpec> entries = new HashMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            entries.put(entry.getKey(), new TarEntrySpec(entry.getValue(), 0644));
        }
        return createTarballWithModes(entries);
    }

    private byte[] createTarballWithModes(Map<String, TarEntrySpec> files) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(outputStream)) {
            tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, TarEntrySpec> entry : files.entrySet()) {
                byte[] content = entry.getValue().contents().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setMode(entry.getValue().mode());
                tarEntry.setSize(content.length);
                tarOutput.putArchiveEntry(tarEntry);
                tarOutput.write(content);
                tarOutput.closeArchiveEntry();
            }
            tarOutput.finish();
        }
        return outputStream.toByteArray();
    }

    private record TarEntrySpec(String contents, int mode) {
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private static class InMemoryTemplateStorageClient implements TemplateStorageClient {

        private final byte[] tarballBytes;
        private int fetchCount;

        private InMemoryTemplateStorageClient(byte[] tarballBytes) {
            this.tarballBytes = tarballBytes;
        }

        @Override
        public TemplateTarball getTemplateTarball(String templateId, String version, String s3Key) {
            fetchCount++;
            return new TemplateTarball(
                    templateId,
                    version,
                    s3Key,
                    tarballBytes.length,
                    new ByteArrayInputStream(tarballBytes)
            );
        }

        private int getFetchCount() {
            return fetchCount;
        }
    }
}
