package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageClient;
import net.spookly.kodama.nodeagent.template.storage.TemplateTarball;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
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
        TemplateCacheLayout layout = createLayout();
        TemplateCachePopulateService service = createService(storageClient, layout);

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

    private TemplateCachePopulateService createService(TemplateStorageClient storageClient, TemplateCacheLayout layout) {
        TemplateCacheLookupService lookupService = new TemplateCacheLookupService(layout);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new TemplateCachePopulateService(layout, lookupService, storageClient, mapper);
    }

    private TemplateCacheLayout createLayout() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());
        return new TemplateCacheLayout(config);
    }

    private byte[] createTarball(Map<String, String> files) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(outputStream)) {
            tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, String> entry : files.entrySet()) {
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setSize(content.length);
                tarOutput.putArchiveEntry(tarEntry);
                tarOutput.write(content);
                tarOutput.closeArchiveEntry();
            }
            tarOutput.finish();
        }
        return outputStream.toByteArray();
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
