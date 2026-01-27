package net.spookly.kodama.nodeagent.template.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateCacheLookupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsHitWhenChecksumMatches() throws Exception {
        TemplateCacheLookupService service = createService();
        TemplateCacheLayout layout = createLayout();
        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");

        Files.createDirectories(paths.contentsDir());
        Files.writeString(paths.checksumFile(), "abc123\n");

        TemplateCacheLookupResult result = service.findCachedTemplate("starter", "1.2.3", "abc123");

        assertThat(result.isCacheHit()).isTrue();
        assertThat(result.missReason()).isNull();
        assertThat(result.contentsDir()).isEqualTo(paths.contentsDir());
        assertThat(result.cachedChecksum()).isEqualTo("abc123");
    }

    @Test
    void returnsMissWhenChecksumDiffers() throws Exception {
        TemplateCacheLookupService service = createService();
        TemplateCacheLayout layout = createLayout();
        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");

        Files.createDirectories(paths.contentsDir());
        Files.writeString(paths.checksumFile(), "abc123");

        TemplateCacheLookupResult result = service.findCachedTemplate("starter", "1.2.3", "def456");

        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.missReason()).isEqualTo(TemplateCacheMissReason.CHECKSUM_MISMATCH);
        assertThat(result.cachedChecksum()).isEqualTo("abc123");
    }

    @Test
    void returnsMissWhenCacheEntryIsMissing() {
        TemplateCacheLookupService service = createService();

        TemplateCacheLookupResult result = service.findCachedTemplate("starter", "1.2.3", "abc123");

        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.missReason()).isEqualTo(TemplateCacheMissReason.NOT_FOUND);
    }

    @Test
    void returnsMissWhenChecksumFileIsMissing() throws Exception {
        TemplateCacheLookupService service = createService();
        TemplateCacheLayout layout = createLayout();
        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");

        Files.createDirectories(paths.contentsDir());

        TemplateCacheLookupResult result = service.findCachedTemplate("starter", "1.2.3", "abc123");

        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.missReason()).isEqualTo(TemplateCacheMissReason.NOT_FOUND);
    }

    @Test
    void returnsMissWhenContentsDirIsMissing() throws Exception {
        TemplateCacheLookupService service = createService();
        TemplateCacheLayout layout = createLayout();
        TemplateCachePaths paths = layout.resolveTemplateVersion("starter", "1.2.3");

        Files.createDirectories(paths.versionRoot());
        Files.writeString(paths.checksumFile(), "abc123");

        TemplateCacheLookupResult result = service.findCachedTemplate("starter", "1.2.3", "abc123");

        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.missReason()).isEqualTo(TemplateCacheMissReason.NOT_FOUND);
    }

    @Test
    void rejectsBlankExpectedChecksum() {
        TemplateCacheLookupService service = createService();

        assertThatThrownBy(() -> service.findCachedTemplate("starter", "1.2.3", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedChecksum");
    }

    private TemplateCacheLookupService createService() {
        return new TemplateCacheLookupService(createLayout());
    }

    private TemplateCacheLayout createLayout() {
        NodeConfig config = new NodeConfig();
        config.setCacheDir(tempDir.resolve("cache-root").toString());
        return new TemplateCacheLayout(config);
    }
}
